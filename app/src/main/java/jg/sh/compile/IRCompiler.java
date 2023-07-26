package jg.sh.compile;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.common.Location;
import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompContext.ContextType;
import jg.sh.compile.CompContext.IdentifierInfo;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.CommentInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.LoadStorePair;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.instrs.StoreInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.ConstantPool.MutableIndex;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.CodeObject;
import jg.sh.compile.pool.component.DataRecord;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.pool.component.StringConstant;
import jg.sh.parsing.Module;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.ArrayLiteral;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.ConstAttrDeclr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.ObjectLiteral;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.statements.CaptureStatement;
import jg.sh.parsing.nodes.statements.DataDefinition;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.ThrowStatement;
import jg.sh.parsing.nodes.statements.UseStatement;
import jg.sh.parsing.nodes.statements.VarDeclr;
import jg.sh.parsing.nodes.statements.VarDeclrList;
import jg.sh.parsing.nodes.statements.blocks.Block;
import jg.sh.parsing.nodes.statements.blocks.IfBlock;
import jg.sh.parsing.nodes.statements.blocks.TryCatch;
import jg.sh.parsing.nodes.statements.blocks.WhileBlock;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;
import jg.sh.parsing.token.TokenType;
import jg.sh.util.Pair;
import jg.sh.compile.results.ConstantResult;
import jg.sh.compile.results.FuncResult;
import jg.sh.compile.results.NodeResult;
import jg.sh.compile.results.VarResult;

import static jg.sh.compile.results.NodeResult.*;
import static jg.sh.compile.instrs.OpCode.*;

public class IRCompiler implements NodeVisitor<NodeResult, CompContext> {

  private static final Logger LOG = LogManager.getLogger(IRCompiler.class);

  public IRCompiler() {}

  public CompilerResult compileModule(Module module) {
    final ArrayList<Instruction> instrs = new ArrayList<>();
    final ArrayList<ValidationException> exceptions = new ArrayList<>();

    final ConstantPool constantPool = new ConstantPool();
    final CompContext moduleContext = new CompContext(ContextType.MODULE, constantPool);

    /*
     * Module var allocator (any variable declared at top-level will be compiled using this)
     */
    final VarAllocator allocator = (name, start, end) -> {
      final StringConstant moduleVarName = constantPool.addString(name);
      return new LoadStorePair(moduleVarName.linkInstr(new LoadInstr(start, end, LOADMV, moduleVarName.getIndex().getIndex())), 
                               moduleVarName.linkInstr(new StoreInstr(start, end, STOREMV, moduleVarName.getIndex().getIndex())));
    };
    moduleContext.setContextValue(ContextKey.VAR_ALLOCATOR, allocator);

    /*
     * Unbounded functions and toplevel statements, when referring to "self", means
     * they're referring to the module instance.
     */
    final Instruction [] moduleLoadInstr = {new LoadInstr(Location.DUMMY, Location.DUMMY, LOADMOD, -1)};
    moduleContext.setContextValue(ContextKey.SELF_CODE, moduleLoadInstr);

    /*
     * First few instrs: module start label
     */
    instrs.add(new CommentInstr("<-- Module Start -->"));
    final LabelInstr moduleStart = new LabelInstr(Location.DUMMY, Location.DUMMY, "moduleStart_"+module.getName());
    instrs.add(moduleStart);

    /*
     * A map of taken top-level symbols and their initial uses.
     */
    final Set<Identifier> takenTopLevelSymbols = new HashSet<>();

    /*
     * Compile import statements
     */
    for (UseStatement useStatement : module.getImports()) {
      final VarResult result = (VarResult) useStatement.accept(this, moduleContext);

      /*
       * Iterate through compiled component imports and add them into
       * our module context
       */
      for (Entry<Identifier, LoadStorePair> res : result.getVars().entrySet()) {
        if (takenTopLevelSymbols.contains(res.getKey())) {
          exceptions.add(new ValidationException("'"+res.getKey()+"' is already a top-level symbol.", 
                                                 res.getKey().start,
                                                 res.getKey().end));
        }
        else {
          takenTopLevelSymbols.add(res.getKey());
          instrs.addAll(result.getInstructions());
          moduleContext.addVariable(res.getKey().getIdentifier(), res.getValue());
        }
      }
    }

    /**
     * Top-level statements that need to be compiled
     * 
     * Its a list of tuples - a Statement and their NodeResult.
     * Since we're doing semantic checking (validation) and VM generation
     * in one phase, we need to preserve the order components are compiled.
     * 
     * With top-level statements, variables can be readily validated and compiled, but 
     * for proper validation, their context maps need to correctly have all accesible symbols.
     * 
     * So, we pre-load the module context with all symbols prior to a variable statement, compile + validate the
     * variable, but we need to "push down" the produced instructions for the variable so that all other 
     * compnents aren't in the wrong place.
     * 
     * This lsit of Pair<Statement, NodeResult> allows us to keep track of which statements
     * still need compilation + validation.
     */
    final ArrayList<Pair<Statement, NodeResult>> toCompile = new ArrayList<>();

    final ArrayDeque<Statement> topLevelStatements = new ArrayDeque<>(module.getStatements());

    /**
     * Iterate through statements and for functions and data definitions,
     * generate load and store instructions, but don't compile their bodies.
     * 
     * For Variable declarations, do both.
     */
    while (!topLevelStatements.isEmpty()) {
      final Statement statement = topLevelStatements.pop();
      if(statement instanceof DataDefinition){
        final DataDefinition dataDef = (DataDefinition) statement;

        LOG.info(" ===> saving datadef "+dataDef.getName()+" for later!");

        final LoadStorePair dataDefModVar = allocator.generate(dataDef.getName().getIdentifier(), 
                                                               dataDef.getName().start, 
                                                               dataDef.getName().end);

        if (takenTopLevelSymbols.contains(dataDef.getName())) {
          exceptions.add(new ValidationException("'"+dataDef.getName().getIdentifier()+"' is already a top-level symbol.", 
                                                 dataDef.getName().start, 
                                                 dataDef.getName().end));
        }
        else {
          takenTopLevelSymbols.add(dataDef.getName());
          toCompile.add(new Pair<>(dataDef, null));
          moduleContext.addVariable(dataDef.getName().getIdentifier(), dataDefModVar);
        }
      }
      else if(statement.getExpr() instanceof FuncDef) {
        final FuncDef func = (FuncDef) statement.getExpr();

        LOG.info(" ===> saving module function "+func.getBoundName()+" for later!");

        final LoadStorePair funcDefModVar = allocator.generate(func.getBoundName().getIdentifier(), 
                                                               func.getBoundName().start, 
                                                               func.getBoundName().end);

        //Top level functions will always have a bound name.
        if (takenTopLevelSymbols.contains(func.getBoundName())) {
          exceptions.add(new ValidationException("'"+func.getBoundName().getIdentifier()+"' is already a top-level symbol.", 
                                                 func.getBoundName().start, 
                                                 func.getBoundName().end));
        }
        else {
          takenTopLevelSymbols.add(func.getBoundName());
          toCompile.add(new Pair<>(statement, null));
          moduleContext.addVariable(func.getBoundName().getIdentifier(), funcDefModVar);
        }
      }
      else if(statement instanceof VarDeclr) {
        final VarDeclr varDeclr = (VarDeclr) statement;

        LOG.info(" ===> compiling module variable "+varDeclr.getName()+" NOW!");

        /**
         * Unlike FuncDef and DataDef, we don't generate load/store instructions
         * for a VarDeclr for validation reasons. If we do, the variable will be present
         * in CompContext (a.k.a, symbol table), making potential self-references and
         * bad references legal. We want to be able to report such cases as ValidationExceptions-
         */
        final VarResult varResult = (VarResult) varDeclr.accept(this, moduleContext);

        if (takenTopLevelSymbols.contains(varDeclr.getName())) {
          exceptions.add(new ValidationException("'"+varDeclr.getName().getIdentifier()+"' is already a top-level symbol.", 
                                                 varDeclr.getName().start, 
                                                 varDeclr.getName().end));
        }
        else if(varResult.hasExceptions()) {
          LOG.info("--- exceptions!!! "+varResult.getExceptions());
          exceptions.addAll(varResult.getExceptions());
        }
        else {
          LOG.info(" ==> top level lookup for: "+varDeclr.getName()+" gave "+varResult.getVars().get(varDeclr.getName()));

          takenTopLevelSymbols.add(varDeclr.getName());
          moduleContext.addVariable(varDeclr.getName().getIdentifier(), 
                                    varResult.getVars().get(varDeclr.getName()), 
                                    varDeclr.getDescriptors());
          toCompile.add(new Pair<>(statement, varResult));
        }
      }
      else if(statement instanceof VarDeclrList) {
        final VarDeclrList varDeclrList = (VarDeclrList) statement;

        LOG.info(" ===> compiling module variables "+varDeclrList.getVarDeclrs()+" NOW!");

        final ArrayList<VarDeclr> vars = new ArrayList<>(varDeclrList.getVarDeclrs());
        for (int i = vars.size() - 1; i >= 0; i--) {
          topLevelStatements.addFirst(vars.get(i));
        }
      }
      else {
        toCompile.add(new Pair<>(statement, null));
      }
    }

    /*
     * Now, compile + validate all components.
     * 
     * All module variables have been compiled + validated at this point
     */
    for (Pair<Statement, NodeResult> result : toCompile) {
      LOG.info(" ===> compiling: "+result.first+" "+(result.second != null));
      if (result.first instanceof DataDefinition) {
        final DataDefinition dataDef = (DataDefinition) result.first;
        final IdentifierInfo dataDefInfo = moduleContext.getVariable(dataDef.getName().getIdentifier());
        final NodeResult dataResult = dataDef.accept(this, moduleContext);

        if (dataResult.hasExceptions()) {
          exceptions.addAll(dataResult.getExceptions());
        }
        else {
          instrs.addAll(dataResult.getInstructions());
        }

        //Load the code object as a module variable
        instrs.add(dataDefInfo.getStoreInstr());
      }
      else if (result.first.getExpr() instanceof FuncDef) {
        final FuncDef func = (FuncDef) result.first.getExpr();
        final IdentifierInfo funcIdenInfo = moduleContext.getDirect(func.getBoundName().getIdentifier());
        final NodeResult funcResult = func.accept(this, moduleContext);

        funcResult.pipeErr(exceptions).pipeInstr(instrs);

        //Load the code object as a module variable
        instrs.add(funcIdenInfo.getStoreInstr());

        if (func.toExport()) {
          instrs.add(new ArgInstr(func.start, func.end, EXPORTMV, funcIdenInfo.getStoreInstr().getIndex()));
        }
      }
      else if(result.first instanceof VarDeclr) {
        //This component has already been validated + compiled 
        final VarResult vResult = (VarResult) result.second;

        //Add the instrs of the variable's value
        instrs.addAll(vResult.getInstructions());
      }
      else {        
        final NodeResult stmtResult = result.first.accept(this, moduleContext);
        if (stmtResult.hasExceptions()) {
          exceptions.addAll(stmtResult.getExceptions());
        }
        else {
          instrs.addAll(stmtResult.getInstructions());
        }
      }
    }

    final ObjectFile objectFile = new ObjectFile(module.getName(), moduleStart.getName(), constantPool, instrs);
    return exceptions.isEmpty() ? new CompilerResult(objectFile) : new CompilerResult(exceptions);
  }

  @Override
  public NodeResult visitStatement(CompContext parentContext, Statement statement) {
    return statement.getExpr() == null ? 
              valid(new NoArgInstr(statement.start, statement.end, PASS)) : 
              statement.getExpr().accept(this, parentContext);
  }

  @Override
  public NodeResult visitThrowStatement(CompContext parentContext, ThrowStatement throwStatement) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    throwStatement.accept(this, parentContext).pipeErr(exceptions).pipeInstr(instrs);

    instrs.add(new NoArgInstr(throwStatement.start, throwStatement.end, RETE));

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public VarResult visitUseStatement(CompContext parentContext, UseStatement useStatement) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final Identifier moduleName = useStatement.getModuleName();
    final VarAllocator varAllocator = (VarAllocator) parentContext.getValue(ContextKey.VAR_ALLOCATOR);

    //Allocate constant string for module name
    final StringConstant moduleNameConstant = constantPool.addString(moduleName.getIdentifier());
    final LoadInstr moduleNameLoad = moduleNameConstant.linkInstr(new LoadInstr(moduleName.start, 
                                                                                moduleName.end, 
                                                                                LOADMOD, 
                                                                                moduleNameConstant.getExactIndex()));    
    if(useStatement.getCompAliasMap().isEmpty()) {
      /*
       * If the useStatement isn't importing any components, then
       * just import the module itself
       */

      //The variable name to refer to the imported module
      final Identifier moduleVarHandle = useStatement.getModuleAlias() != null ?
                                           useStatement.getModuleAlias() : 
                                           moduleName;
      /*
       * Use statements are just top-level variables 
       * whose values are the module instance (and components, if specificed)
       * 
       * use module1; <=> module1 = load("module1")  //internal function
       * use module1 as m;  <=>  m = load("module1")
       * 
       * use module1::comp, boo; <=> comp = load("module1").comp; etc....
       * use module1::comp as c; <=> c = load("module1").comp 
       * 
       * Note: the implementation of load() 
       */
      final LoadStorePair loadStore = varAllocator.generate(moduleVarHandle.getIdentifier(), 
                                                            moduleVarHandle.start, 
                                                            moduleVarHandle.end);
      return VarResult.single(moduleVarHandle, 
                              loadStore, 

                              //Load Module and store it in respective module variable
                              new CommentInstr("*** Bare loading of "+useStatement.getModuleName().getIdentifier()+""),
                              moduleNameLoad,
                              loadStore.store);
    }
    else {
      final LinkedHashMap<Identifier, LoadStorePair> compMap = new LinkedHashMap<>();
      final ArrayList<Instruction> instrs = new ArrayList<>();

      /*
       * Iterate through imported components.
       */
      for (Entry<Identifier, Identifier> compEntry : useStatement.getCompAliasMap().entrySet()) {
        instrs.add(new CommentInstr("*** Importing '"+compEntry.getKey()+"' from "+moduleName.getIdentifier()+" ***"));

        //Load module
        instrs.add(moduleNameLoad);

        //The variable name to refer to the imported module component
        final Identifier moduleVarHandle = compEntry.getValue() != null ?
                                            compEntry.getValue() : 
                                            compEntry.getKey();

        final StringConstant compNameConstant = constantPool.addString(compEntry.getKey().getIdentifier());
        final LoadInstr compLoadInstr = compNameConstant.linkInstr(new LoadInstr(compEntry.getKey().start, 
                                                                                 compEntry.getKey().end, 
                                                                                 LOADATTR, 
                                                                                 compNameConstant.getExactIndex()));

        //Load and store instructions for impoted module components
        final LoadStorePair loadStore = varAllocator.generate(moduleVarHandle.getIdentifier(), 
                                                              moduleVarHandle.start, 
                                                              moduleVarHandle.end);

        //Load the specific attribute so it can be stored in our module
        instrs.add(compLoadInstr);
        
        instrs.add(loadStore.store);
        compMap.put(moduleVarHandle, loadStore);
      }

      return new VarResult(Collections.emptyList(), compMap, instrs);
    }
  }

  @Override
  public NodeResult visitReturnStatement(CompContext parentContext, ReturnStatement returnStatement) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    if (returnStatement.hasValue()) {
      LOG.info(" COMPILING RETURN: "+returnStatement.getValue());
      returnStatement.getExpr().accept(this, parentContext)
                               .pipeErr(exceptions)
                               .pipeInstr(instrs);
    }
    else {
      instrs.add(new NoArgInstr(returnStatement.start, returnStatement.end, LOADNULL));
    }

    instrs.add(new NoArgInstr(returnStatement.start, returnStatement.end, RET));
    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitDataDefinition(CompContext parentContext, DataDefinition dataDefinition) {
    /**
     * Data definitions are just syntactic sugar templated object creation.
     * 
     * Given this:
     * 
     * data [export] [sealed] <dataTypeName> {
     *  
     *   [ constr([parameter, ..... ]) {
     *       statements....
     *     }
     *   ]
     *     
     *   (
    *      func methodName([parameter, ...]) {
     *          statements....
     *      }
     *   )*
     * }
     * 
     * we want to output this:
     * func [export] <dataTypeName> ([parameter, ....]) {
     *    const obj := object {
     *       const $type := dataTypeRecord;  //This created at compilation
     *       [const] parameter1 := parameter1; 
     *       .....
     *    };
     * 
     *    statements....
     *    
     *    return obj; 
     * }
     */

    final ConstantPool pool = parentContext.getConstantPool();
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();
    
    final FuncDef constructor = dataDefinition.getConstructor();

    //Convert attribute descriptors to numbers
    final LinkedHashMap<String, MutableIndex> methods = new LinkedHashMap<>();

    for (FuncDef method : dataDefinition.getMethods().values()) {
      /*
       * visitFuncDef will have instructions for function
       * instatiation, while also creating a CodeObject.
       * 
       * As part of these instructions, it will bind our methods
       * to the module object.
       * 
       * We don't want nor need this, so we'll ignore the instructions
       * visitFuncDef generates and just getting the CodeObject it makes. 
       */
      final FuncResult result = (FuncResult) method.accept(this, parentContext);
      result.pipeErr(exceptions);
      methods.put(method.getBoundName().getIdentifier(), result.getCodeObjectIndex());
    }

    //Allocate DataRecord on constant pool
    final DataRecord dataRecord = pool.addDataRecord(dataDefinition.getName().getIdentifier(), 
                                                       constructor.getSignature(), 
                                                       methods, 
                                                       dataDefinition.isSealed());

    /*
     * One thing to note about DataDefinitions/records. 
     * 
     * While it's a DataDef is a distinct object at runtime,
     * it acts as both a symbol and a function.
     * 
     * If the interpreter sees that we're invoking - making a function call
     * - to a DataDefinition (a.k.a a RuntimeDataRecord), it will automatically
     * deduce that we're instantiating an object based on that DataDefinition.
     * 
     * In that case, the interpreter will allocate an empty object first and pass it on
     * to the constructor as its "self" object - engaging the usual function calling convention.
     */

    instrs.add(dataRecord.linkInstr(new LoadInstr(dataDefinition.start, 
                                                  dataDefinition.end, 
                                                  LOADC, 
                                                  dataRecord.getExactIndex())));
    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitCaptureStatement(CompContext parentContext, CaptureStatement captureStatement) {
    /*
     * This shouldn't be visited as the capture statement of
     * functions and constructors are removed from their statement list
     */
    throw new UnsupportedOperationException("Unimplemented method 'visitCaptureStatement'");
  }

  @Override
  public NodeResult visitBlock(CompContext parentContext, Block block) {
    LOG.info(" ===> in block: "+block.start+" | "+block.end);
    return compileStatements(block.getStatements(), parentContext, block.start, block.end);
  }

  private NodeResult compileStatements(List<Statement> stmts, CompContext parentContext, Location start, Location end) {
    LOG.info("--- compiling statements: "+stmts);
    final ArrayDeque<Statement> stmtDeque = new ArrayDeque<>(stmts);

    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final CompContext blockContext = new CompContext(parentContext, ContextType.BLOCK);
    final VarAllocator allocator = (VarAllocator) blockContext.getValue(ContextKey.VAR_ALLOCATOR);

    final ArrayList<Pair<Statement, NodeResult>> toCompile = new ArrayList<>();

    /**
     * Iterate through statements and look for functions, generate load and store instructions, but don't compile their bodies.
     * 
     * For Variable declarations, do both.
     */
    while (!stmtDeque.isEmpty()) {
      final Statement stmt = stmtDeque.poll();

      LOG.info("  ==> stmt class? "+stmt.getClass());

      if (stmt.getExpr() instanceof FuncDef) {
        final FuncDef func = (FuncDef) stmt.getExpr();
        if (func.hasBoundName()) {
          final LoadStorePair pair = allocator.generate(func.getBoundName().getIdentifier(), func.start, func.end);
          blockContext.addVariable(func.getBoundName().getIdentifier(), pair);
        }

        toCompile.add(new Pair<>(stmt, null));
      }
      else if(stmt instanceof VarDeclr) {
        final VarDeclr varDeclr = (VarDeclr) stmt;
        final IdentifierInfo existingVar = blockContext.getVariable(varDeclr.getName().getIdentifier());

        LOG.info(" ===> compiling var: "+varDeclr);

        if (existingVar != null && existingVar.getContext().getCurrentContext() != ContextType.MODULE) {
          /*
           * Check if a variable in the current scope (or any enclosing one) of the same name exists.
           * If so, this is a validation error, unless the original variable is top-evel (module)
           */
          exceptions.add(new ValidationException("'"+varDeclr.getName().getIdentifier()+"' has already been declared.", 
                                                 varDeclr.getName().start, 
                                                 varDeclr.getName().end));
        }

        final VarResult varResult = (VarResult) varDeclr.accept(this, blockContext);

        if(varResult.hasExceptions()) {
          exceptions.addAll(varResult.getExceptions());
        }
        else {
          blockContext.addVariable(varDeclr.getName().getIdentifier(), 
                                   varResult.getVars().get(varDeclr.getName()), 
                                   varDeclr.getDescriptors());
          toCompile.add(new Pair<>(stmt, varResult));
        }
      }
      else if(stmt instanceof VarDeclrList) {
        final VarDeclrList varDeclrList = (VarDeclrList) stmt;

        //LOG.info(" ===> compiling variables "+varDeclrList.getVarDeclrs()+" NOW!");

        final ArrayList<VarDeclr> vars = new ArrayList<>(varDeclrList.getVarDeclrs());
        for (int i = vars.size() - 1; i >= 0; i--) {
          stmtDeque.addFirst(vars.get(i));
        }
      }
      else {
        toCompile.add(new Pair<>(stmt, null));
      }
    }

    //Now, compile everything
    for (Pair<Statement, NodeResult> target : toCompile) {
      instrs.add(new CommentInstr(" => For statement at: "+target.first.start+" | "+target.first.repr()));

      if (target.first.getExpr() instanceof FuncDef) {
        final FuncDef func = (FuncDef) target.first.getExpr();

        func.accept(this, blockContext).pipeErr(exceptions).pipeInstr(instrs);

        if (func.hasBoundName()) {
          /*
           * Store function to a local var if it has a bound name
           */
          final IdentifierInfo funcIdenInfo = blockContext.getDirect(func.getBoundName().getIdentifier());
          instrs.add(funcIdenInfo.getStoreInstr());
        }
      }
      else if(target.first instanceof VarDeclr) {
        //This component has already been validated + compiled 
        final VarResult vResult = (VarResult) target.second;

        //Add the instrs of the variable's value
        instrs.addAll(vResult.getInstructions());
      }
      else {        
        target.first.accept(this, blockContext).pipeErr(exceptions).pipeInstr(instrs);
      }
    }

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitIfBlock(CompContext parentContext, IfBlock ifBlock) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //The label each branch jumps to upon completion
    final String doneLabel = genLabelName("done");
    
    //compile the initial branch code
    final String initBranch = genLabelName("initBranch");
    instrs.add(new LabelInstr(ifBlock.start, ifBlock.end, initBranch));

    ifBlock.getCondition().accept(this, parentContext)
                          .pipeErr(exceptions)
                          .pipeInstr(instrs);
    
    //Make new ContextManager for IfElse
    //final ContextManager firstIfContext = new ContextManager(contextManager, ContextType.BLOCK);
    
    //If there's no other branches, just jump to the done label
    if(ifBlock.getOtherBranches().isEmpty()) {
      instrs.add(new JumpInstr(ifBlock.start, ifBlock.end, OpCode.JUMPF, doneLabel));

      compileStatements(ifBlock.getStatements(), 
                        parentContext, 
                        ifBlock.start, 
                        ifBlock.end).pipeErr(exceptions)
                                    .pipeInstr(instrs);
    }
    else {
      //Label for the next branch
      String nextLabel = genLabelName("branch");
      
      //if Condition is false, jump to the next branch's label
      instrs.add(new JumpInstr(ifBlock.start, ifBlock.end, OpCode.JUMPF, nextLabel));   
      instrs.add(new CommentInstr(ifBlock.start, ifBlock.end, "For line #"+ifBlock.start));
      
      compileStatements(ifBlock.getStatements(), 
                        parentContext, 
                        ifBlock.start, 
                        ifBlock.end).pipeErr(exceptions)
                                    .pipeInstr(instrs);

      instrs.add(new JumpInstr(ifBlock.start, ifBlock.end, OpCode.JUMP, doneLabel));
      instrs.add(new CommentInstr(" ===> END OF INIT IF"));
      
      for (int i = 0; i < ifBlock.getOtherBranches().size(); i++) {
        final IfBlock currentBranch = ifBlock.getOtherBranches().get(i);

        //Add branch label first
        instrs.add(new LabelInstr(currentBranch.start, currentBranch.end, nextLabel));
        
        //generate label name for the next branch
        nextLabel = genLabelName("branch");
        
        //Make new ContextManager for other IfElse
        //final ContextManager otherBranchContext = new ContextManager(contextManager, ContextType.BLOCK);
        
        //This is an elif block
        if(currentBranch.getCondition() != null) {
          currentBranch.getCondition().accept(this, parentContext)
                                      .pipeErr(exceptions)
                                      .pipeInstr(instrs);
          
          //If this is the last elif block, then jump to done. Else, jump to the next elif/else label
          String nextLabelToJump = i == ifBlock.getOtherBranches().size() - 1 ? doneLabel : nextLabel;
          instrs.add(new JumpInstr(currentBranch.start, currentBranch.end, OpCode.JUMPF, nextLabelToJump));
        }

        //Compile branch statements
        compileStatements(currentBranch.getStatements(), 
                          parentContext, 
                          currentBranch.start, 
                          currentBranch.end).pipeErr(exceptions)
                                            .pipeInstr(instrs);
        
        //If this is an elif block, jump to the done label after this branch is done executing.
        if(currentBranch.getCondition() != null) {
          instrs.add(new JumpInstr(currentBranch.start, currentBranch.end, OpCode.JUMP, doneLabel));
        }
      }
    }
    
    //Add done labal
    instrs.add(new LabelInstr(ifBlock.start, ifBlock.end, doneLabel));

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitTryCatchBlock(CompContext parentContext, TryCatch tryCatch) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final ConstantPool constantPool = parentContext.getConstantPool();
    final VarAllocator allocator = (VarAllocator) parentContext.getValue(ContextKey.VAR_ALLOCATOR);

    final String tryStartLabel = genLabelName("tryStart");
    final String tryEndLabel = genLabelName("tryEnd");
    final String catchLabel = genLabelName("catch");
    
    //Make an ErrorHandlingRecord for this try-catch block
    constantPool.addErrorHandling(tryStartLabel, tryEndLabel, catchLabel);
    
    //add instructions for try block first
    instrs.add(new LabelInstr(tryCatch.start, tryCatch.end, tryStartLabel));
    compileStatements(tryCatch.getStatements(), 
                      parentContext, 
                      tryCatch.start, 
                      tryCatch.end).pipeErr(exceptions)
                                   .pipeInstr(instrs);
    instrs.add(new LabelInstr(tryCatch.start, tryCatch.end, tryEndLabel));

    //Create new CompContext for catch-block
    final CompContext catchContext = new CompContext(parentContext, ContextType.BLOCK);

    //Compile the catch part    
    //Add label for catch block
    instrs.add(new LabelInstr(tryCatch.start, tryCatch.end, catchLabel));
    
    final Identifier errorIdentifier = tryCatch.getExceptionHandler();
    final LoadStorePair errorHandlerInstrs = allocator.generate(errorIdentifier.getIdentifier(), 
                                                               errorIdentifier.start, 
                                                               errorIdentifier.end);
    
    //Add instructions for storing the error object
    instrs.add(new NoArgInstr(errorIdentifier.start, errorIdentifier.end, OpCode.POPERR));
    instrs.add(errorHandlerInstrs.store);
    
    //add error variable to the context manager
    catchContext.addVariable(errorIdentifier.getIdentifier(), errorHandlerInstrs);

    tryCatch.getCatchBlock().accept(this, catchContext)
                            .pipeErr(exceptions)
                            .pipeInstr(instrs);

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitWhileBlock(CompContext parentContext, WhileBlock whileBlock) {
    LOG.info("=== WHILE BLOCK: "+whileBlock.repr());
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final String loopLabel = genLabelName("while_loop");
    final String endLabel = genLabelName("loop_end");
    
    //Loop context
    final CompContext loopContext = new CompContext(parentContext, ContextType.LOOP);
    loopContext.setContextValue(ContextKey.CONT_LOOP_LABEL, loopLabel);
    loopContext.setContextValue(ContextKey.BREAK_LOOP_LABEL, endLabel);
    
    //Create label for while loop
    instrs.add(new LabelInstr(whileBlock.start, whileBlock.end, loopLabel));
    
    //Add instructions for loop condition first
    whileBlock.getCondition().accept(this, parentContext)
                             .pipeErr(exceptions)
                             .pipeInstr(instrs);
    
    instrs.add(new JumpInstr(whileBlock.start, whileBlock.end, OpCode.JUMPF, endLabel));
    
    compileStatements(whileBlock.getStatements(), 
                      loopContext, 
                      whileBlock.start, 
                      whileBlock.end).pipeErr(exceptions)
                                     .pipeInstr(instrs);
    
    instrs.add(new JumpInstr(whileBlock.start, whileBlock.end, OpCode.JUMP, loopLabel));
    instrs.add(new LabelInstr(whileBlock.start, whileBlock.end, endLabel));

    LOG.info(" === WHILE BLOCK RESULT: "+instrs.stream().map(Instruction::toString).collect(Collectors.joining(System.lineSeparator())));

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public ConstantResult visitString(CompContext parentContext, Str str) {
    final ConstantPool pool = parentContext.getConstantPool();
    final StringConstant constant = pool.addString(str.getValue());
    final LoadInstr loadInstr = constant.linkInstr(new LoadInstr(str.start, str.end, LOADC, constant.getExactIndex()));
    return new ConstantResult(constant, loadInstr);
  }

  @Override
  public ConstantResult visitInt(CompContext parentContext, Int integer) {
    final ConstantPool pool = parentContext.getConstantPool();
    final IntegerConstant constant = pool.addInt(integer.getValue());
    final LoadInstr loadInstr = constant.linkInstr(new LoadInstr(integer.start, integer.end, LOADC, constant.getExactIndex()));
    return new ConstantResult(constant, loadInstr);  
  }

  @Override
  public ConstantResult visitBoolean(CompContext parentContext, Bool bool) {
    final ConstantPool pool = parentContext.getConstantPool();
    final BoolConstant constant = pool.addBoolean(bool.getValue());
    final LoadInstr loadInstr = constant.linkInstr(new LoadInstr(bool.start, bool.end, LOADC, constant.getExactIndex()));
    return new ConstantResult(constant, loadInstr);
  }

  @Override
  public ConstantResult visitFloat(CompContext parentContext, FloatingPoint floatingPoint) {
    final ConstantPool pool = parentContext.getConstantPool();
    final FloatConstant constant = pool.addFloat(floatingPoint.getValue());
    final LoadInstr loadInstr = constant.linkInstr(new LoadInstr(floatingPoint.start, floatingPoint.end, LOADC, constant.getExactIndex()));
    return new ConstantResult(constant, loadInstr);
  }

  @Override
  public NodeResult visitNull(CompContext parentContext, Null nullVal) {
    return valid(new NoArgInstr(nullVal.start, nullVal.end, LOADNULL));
  }

  @Override
  public NodeResult visitIdentifier(CompContext parentContext, Identifier identifier) {
    final IdentifierInfo identifierInfo = parentContext.getVariable(identifier.getIdentifier());

    if (identifierInfo == null) {
      return invalid(new ValidationException("'"+identifier.getIdentifier()+"' is unfound.", 
                                             identifier.start, 
                                             identifier.end));
    }

    if (parentContext.hasContextValue(ContextKey.NEED_STORAGE) && (boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      if (identifierInfo.isConstant()) {
        return invalid(new ValidationException("'"+identifier.getIdentifier()+"' is constant and cannot be re-assigned.", 
                                               identifier.start, 
                                               identifier.end));
      }
      return valid(identifierInfo.getStoreInstr());
    }
    return valid(identifierInfo.getLoadInstr());
  }

  @Override
  public NodeResult visitKeyword(CompContext parentContext, Keyword keyword) {
    // return; and return <expr>; are always wrapped as a ReturnStatement
    switch (keyword.getKeyword()) {
      case BREAK: 
      case CONTINUE: {
        final CompContext loopContext = parentContext.getNearestContext(ContextType.LOOP);
        final CompContext funcContext = parentContext.getNearestContext(ContextType.FUNCTION);

        if (loopContext == null || 
            funcContext.getNearestContext(ContextType.LOOP) == loopContext) {
          /*
           * First check: If the break/continue isn't in any loop at all, then
           *              it's invalid
           *
           * The second check works like this:
           * 
           * We get the contexts corresponding to the nearest loop and function. A break and continue
           * must exist within a loop context, so if there's none found, that's a trivial error.
           * 
           * But to check for the more difficult condition: when our break and continue is within a function
           * that's inside a loop, we get the context corresponding to the nearest function
           * 
           * We then check the context of the corresponding function - if there is one - if it's within a loop and getting
           * the corresponding context object of it. We then check - by plain reference equality - if this loop context object
           * is the same as the loop context object we had. 
           */
          return invalid(new ValidationException(keyword.repr()+" isn't within a loop.", keyword.start, keyword.end));
        }

        final String targetLabel = (String) parentContext.getValue(keyword.getKeyword() == TokenType.BREAK ? 
                                                                      ContextKey.BREAK_LOOP_LABEL : 
                                                                      ContextKey.CONT_LOOP_LABEL);
        return valid(new JumpInstr(keyword.start, keyword.end, JUMP, targetLabel));
      }
      case MODULE: {
        return valid(new LoadInstr(keyword.start, keyword.end, LOADMOD, -1));
      }
      case SELF: {
        final Instruction [] selfLoad = (Instruction[]) parentContext.getValue(ContextKey.SELF_CODE);

        if(selfLoad == null) {
          return invalid(new ValidationException("'self' is unfound.", keyword.start, keyword.end));
        }

        return valid(selfLoad);
      }
      default: invalid(new ValidationException("Unkown keyword '"+keyword.repr()+"'.", keyword.start, keyword.end));
    }

    //This should never happen.
    return null;
  }

  @Override
  public NodeResult visitParameter(CompContext parentContext, Parameter parameter) {
    return valid();
  }

  /**
   * The generation of load/store instructions for function definitions
   * (whether local or top-level) is the responsibility of the enclosing scope
   * (the host data definition, scope, module, etc.)
   * 
   * The method will look up its assigned name on its parentContext
   * for load/store instructions
   */
  @Override
  public FuncResult visitFuncDef(CompContext parentContext, FuncDef funcDef) {
    LOG.info(" ---> func def: "+funcDef.getBoundName()+" | "+parentContext.getCurrentContext());

    final ConstantPool pool = parentContext.getConstantPool();
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final CompContext funcContext = new CompContext(parentContext, ContextType.FUNCTION);

    //set local var index to 0
    funcContext.setContextValue(ContextKey.LOCAL_VAR_INDEX, 0);
    final VarAllocator localVarAlloc = (name, start, end) -> {
      final int varIndex = (int) funcContext.getValue(ContextKey.LOCAL_VAR_INDEX);
      final LoadStorePair LS = new LoadStorePair(new LoadInstr(start, end, LOAD, varIndex), 
                                                 new StoreInstr(start, end, STORE, varIndex));
      funcContext.setContextValue(ContextKey.LOCAL_VAR_INDEX, varIndex + 1);
      return LS;
    };
    funcContext.setContextValue(ContextKey.VAR_ALLOCATOR, localVarAlloc);

    //set closure index to 0
    funcContext.setContextValue(ContextKey.CL_VAR_INDEX, 0);

    //int[] for captured variables
    final int [] captures = new int[funcDef.getCaptures().size()];

    //index for captured variables
    int captureIndex = 0;

    LOG.info("==> "+funcDef.getBoundName()+" | "+funcDef.start+" | captures: "+funcDef.getCaptures());

    //Compile capture first
    for (Identifier capture : funcDef.getCaptures()) {
      final IdentifierInfo capturedInfo = funcContext.getVariable(capture.getIdentifier());
      if (capturedInfo == null) {
        exceptions.add(new ValidationException("'"+capture.getIdentifier()+"' is unfound.", 
                                               capture.start, 
                                               capture.end));
      }
      else {
        final LoadStorePair capLoadStore = capturedInfo.getPairInstr();

        /*
         * If the identifier is a local variable (not module-level), change the load instruction 
         * to be a LOAD/STORE_CL - if it hasn't yet.
         */
        if(capLoadStore.load.getOpCode() != LOAD_CL && capLoadStore.store.getOpCode() != STORE_CL) {
          capLoadStore.load.setOpCode(LOAD_CL);
          capLoadStore.store.setOpCode(STORE_CL);

          final int closureIndex = (int) capturedInfo.getContext().getValue(ContextKey.CL_VAR_INDEX);
          capLoadStore.load.setIndex(closureIndex);
          capLoadStore.store.setIndex(closureIndex);
          capturedInfo.getContext().setContextValue(ContextKey.CL_VAR_INDEX, closureIndex + 1);
        }

        final LoadStorePair loadStore = new LoadStorePair(new LoadInstr(capture.start, capture.end, LOAD_CL, captureIndex), 
                                                          new StoreInstr(capture.start, capture.end, STORE_CL, captureIndex));

        funcContext.addVariable(capture.getIdentifier(), loadStore);
        captures[captureIndex] = capturedInfo.getLoadInstr().getIndex();
        captureIndex++;
      }
    }

    /*
     * The first local variable - at index 0 - is the function itself
     */
    {
      /*
       * If this function has a bound name, recursion is possible!
       */
      final String funcBoundName = funcDef.hasBoundName() ? 
                                      funcDef.getBoundName().getIdentifier() : 
                                      "$recurse";
      
      final LoadStorePair recurseLS = localVarAlloc.generate(funcBoundName, Location.DUMMY, Location.DUMMY);
      funcContext.addVariable(funcBoundName, recurseLS);
    }

    /*
     * The second local variable - at index 1 - is the self object.
     * 
     * For module functions, the self object is simply the module object
     * 
     * For class functions/methods, the self object is the instance of the class on which the method is being invoked on
     * 
     * For anonymous functions, it varies:
     *  -> if an anonymous function is defined within an object literal, self is the nearest object they're in
     *  -> else, the anonymous function inherits the self of their host function
     */
    {
      final String selfLowerCase = TokenType.SELF.name().toLowerCase();
      final LoadStorePair selfLS = localVarAlloc.generate(selfLowerCase, Location.DUMMY, Location.DUMMY);
      funcContext.addVariable(selfLowerCase, selfLS);

      funcContext.setContextValue(ContextKey.SELF_CODE, new Instruction[]{selfLS.load});
    }

    //Generate label for function
    final String funcLabel = genLabelName(funcDef.hasBoundName() ? funcDef.getBoundName()+"_" : "anonFunc_");
    instrs.add(new LabelInstr(funcDef.start, funcDef.end, funcLabel));

    /*
     * Keeps track of the local var index of keyword parameters. 
     * 
     * This is needed when the function is being called and the caller is preparing the
     * callee's parameter values.
     */
    final Map<String, Integer> keywordParamToIndexMap = new HashMap<>();

    int varArgIndex = -1;
    int keywordVarArgsIndex = -1;

    //Compile parameters
    for (Parameter parameter : funcDef.getParameters().values()) {
      final Identifier paramName = parameter.getName();
      final LoadStorePair paramLS = localVarAlloc.generate(paramName.getIdentifier(), paramName.start, paramName.end);

      if (parameter.hasValue()) {
        //This is an optional parameter
        final Node initValue = parameter.getInitValue();
        final StringConstant keywordNameConstant = pool.addString(paramName.getIdentifier());

        final String keywordCheckDone = genLabelName(paramName.getIdentifier()+"_exists_checkDone");

        instrs.add(keywordNameConstant.linkInstr(new LoadInstr(parameter.start, 
                                                               parameter.end, 
                                                               HAS_KARG, 
                                                               keywordNameConstant.getExactIndex())));
        instrs.add(new JumpInstr(initValue.start, initValue.end, JUMPT, keywordCheckDone));

        initValue.accept(this, funcContext).pipeErr(exceptions).pipeInstr(instrs);
        instrs.add(paramLS.store);

        instrs.add(new LabelInstr(initValue.start, initValue.end, keywordCheckDone));

        keywordParamToIndexMap.put(paramName.getIdentifier(), paramLS.load.getIndex());
      }
      else if(parameter.isVarying()) {
        varArgIndex = paramLS.load.getIndex();
      }
      else if(parameter.isVarArgsKeyword()) {
        keywordVarArgsIndex = paramLS.load.getIndex();
      }


      funcContext.addVariable(paramName.getIdentifier(), paramLS, parameter.getDescriptors());
    }

    //Compile function body
    funcDef.getBody().accept(this, funcContext).pipeErr(exceptions).pipeInstr(instrs);

    //We'll append a "return null" at the end of the function as a catch-all for all execution paths
    instrs.add(new CommentInstr(" -> Catch-all return null <- "));
    instrs.add(new NoArgInstr(Location.DUMMY, Location.DUMMY, LOADNULL));
    instrs.add(new NoArgInstr(Location.DUMMY, Location.DUMMY, RET));

    //Now, allocate this function as a code object instance
    final CodeObject funcCodeObj = pool.addCodeObject(funcDef.getSignature(), 
                                                      funcLabel, 
                                                      keywordParamToIndexMap, 
                                                      varArgIndex, 
                                                      keywordVarArgsIndex,
                                                      instrs, 
                                                      captures);

    //Add on nearest "self" code loading code
    final Instruction [] selfLoadCode = (Instruction[]) parentContext.getValue(ContextKey.SELF_CODE);

    final ArrayList<Instruction> funcLoadingInstrs = new ArrayList<>();
    funcLoadingInstrs.addAll(Arrays.asList(selfLoadCode));
    funcLoadingInstrs.add(funcCodeObj.linkInstr(new LoadInstr(funcDef.end, funcDef.end, LOADC, funcCodeObj.getExactIndex())));
    funcLoadingInstrs.add(new NoArgInstr(funcDef.start, funcDef.end, ALLOCF));

    return new FuncResult(exceptions, funcLoadingInstrs, funcCodeObj.getIndex());
  }

  @Override
  public NodeResult visitOperator(CompContext parentContext, Operator operator) {
    return valid();
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    final ConstantPool pool = parentContext.getConstantPool();
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();
    final Op op = binaryOpExpr.getOperator().getOp();

    LOG.info(" ===> binary expr: "+binaryOpExpr.repr());

    if(op == Op.ASSIGNMENT) {
      /*
       * Compile value expression first
       */
      binaryOpExpr.getRight().accept(this, parentContext).pipeErr(exceptions).pipeInstr(instrs);

      final CompContext leftContext = new CompContext(parentContext, parentContext.getCurrentContext());
      leftContext.setContextValue(ContextKey.NEED_STORAGE, true);

      /*
       * Compile assignee next.
       */
      binaryOpExpr.getLeft().accept(this, leftContext).pipeErr(exceptions).pipeInstr(instrs);
    }
    else if(Op.mutatesLeft(op)) {
      /*
       * Expand expression, from a += b to a = a * b
       * where * is any operator
       */

      LOG.info(" === LEFT: "+binaryOpExpr.getLeft());
      LOG.info(" === RIGHT: "+binaryOpExpr.getRight());

      final BinaryOpExpr valueExpr = new BinaryOpExpr(binaryOpExpr.getLeft(), 
                                                      binaryOpExpr.getRight(), 
                                                      new Operator(Op.getMutatorOperator(op), 
                                                                   binaryOpExpr.getOperator().start, 
                                                                   binaryOpExpr.getOperator().end));
      final BinaryOpExpr assignExpr = new BinaryOpExpr(binaryOpExpr.getLeft(), 
                                                       valueExpr, 
                                                       new Operator(Op.ASSIGNMENT, 
                                                                    binaryOpExpr.getOperator().start, 
                                                                    binaryOpExpr.getOperator().end));
      LOG.info(" === TRANSLATED: "+assignExpr);
      assignExpr.accept(this, parentContext).pipeErr(exceptions).pipeInstr(instrs);
    }
    else if(op == Op.BOOL_AND) {
      final String operandFalse = genLabelName("sc_op_false");
      final String endBranch =  genLabelName("sc_done");

      /*
      * If the left operand is false, jump to operandFalse
      */
      final NodeResult left = binaryOpExpr.getLeft().accept(this, parentContext);
      if (left.hasExceptions()) {
        exceptions.addAll(left.getExceptions());
      }
      else {
        instrs.addAll(left.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPF, operandFalse));
      }

      /**
       * At this point, the left operand must have evaulated to true.
       * Given that, let's evaluate the right operand. If that evaluates to false,
       * jump to operandFalse
       */
      final NodeResult right = binaryOpExpr.getRight().accept(this, parentContext);
      if (right.hasExceptions()) {
        exceptions.addAll(right.getExceptions());
      }
      else {
        instrs.addAll(right.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPF, operandFalse));
      }

      /*
      * At this point, both operands are true. Jump to endBranch
      */
      instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMP, endBranch));

      //operandFalse label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, operandFalse));

      final BoolConstant falseConstant = pool.addBoolean(false);
      instrs.add(falseConstant.linkInstr(new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant.getExactIndex())));

      //endBranch label end
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, endBranch));
    }
    else if(op == Op.BOOL_OR) {
      final String operandTrue = genLabelName("sc_op_true");
      final String endBranch =  genLabelName("sc_done");

      /*
      * If the left operand is true, jump to operandTrue
      */
      final NodeResult left = binaryOpExpr.getLeft().accept(this, parentContext);
      if (left.hasExceptions()) {
        exceptions.addAll(left.getExceptions());
      }
      else {
        instrs.addAll(left.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPT, operandTrue));
      }

      /**
       * At this point, the left operand must have evaulated to false.
       * Given that, let's evaluate the right operand. If that evaluates to true,
       * jump to operandTrue
       */
      final NodeResult right = binaryOpExpr.getRight().accept(this, parentContext);
      if (right.hasExceptions()) {
        exceptions.addAll(right.getExceptions());
      }
      else {
        instrs.addAll(right.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPT, operandTrue));
      }

      //At this point, neither operand is true. Push true and jump to endBranch
      final BoolConstant falseConstant = pool.addBoolean(false);
      instrs.add(falseConstant.linkInstr(new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant.getExactIndex())));
      instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMP, endBranch));

      //operandTrue label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, operandTrue));
      final BoolConstant trueConstant = pool.addBoolean(true);
      instrs.add(falseConstant.linkInstr(new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, trueConstant.getExactIndex())));

      //endBranch label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, endBranch));
    }
    else if(op == Op.ARROW) {
      /**
       * targetExpr -> newSelf (changes what "self" is for the targetExpr, which is expected to be a function)
       * 
       * Internally, this is call to system.bind()
       */
      instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, MAKEARGV));

      /**
       * Compile left operand first.
       */
      final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
      if (leftResult.hasExceptions()) {
        exceptions.addAll(leftResult.getExceptions());
      }
      else {
        instrs.addAll(leftResult.getInstructions());
        instrs.add(new LoadInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, ARG, -1));
      }
            
      /**
       * Compile the right operand next
       */
      final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);
      if (rightResult.hasExceptions()) {
        exceptions.addAll(rightResult.getExceptions());
      }
      else {
        instrs.addAll(rightResult.getInstructions());
        instrs.add(new LoadInstr(binaryOpExpr.getRight().start, binaryOpExpr.getRight().end, ARG, -1));
      }

      //Add bind instruction
      instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, BIND));
    }
    else {
      /**
       * Compile left operand first.
       */
      final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
      leftResult.pipeErr(exceptions).pipeInstr(instrs);
            
      /**
       * Compile the right operand next
       */
      final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);
      rightResult.pipeErr(exceptions).pipeInstr(instrs);


      final OpCode opCode = opToCode(op);
      if (opCode == null) {
        exceptions.add(new ValidationException("'"+op.str+"' is an unknown operator.", 
                                              binaryOpExpr.getOperator().start, 
                                              binaryOpExpr.getOperator().end));
      }
      else {
        instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, opCode));
      }
    }

    //LOG.info("=== binary op expr: "+binaryOpExpr+" instrs: "+instrs);

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  private static OpCode opToCode(Op op) {
    switch (op) {
      case PLUS: return ADD;
      case MINUS: return SUB;
      case MULT: return MUL;
      case DIV: return DIV;
      case MOD: return MOD;
      case LESS: return LESS;
      case GREAT: return GREAT;
      case GR_EQ: return GREATE;
      case LS_EQ: return LESSE;
      case EQUAL: return EQUAL;
      case NOT_EQ: return NOTEQUAL;
      case AND: return BAND;
      case OR: return BOR;
      default: return null;
    }
  }

  @Override
  public NodeResult visitParenthesized(CompContext parentContext, Parenthesized parenthesized) {
    return parenthesized.getInner().accept(this, parentContext);
  }

  @Override
  public NodeResult visitObjectLiteral(CompContext parentContext, ObjectLiteral objectLiteral) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //These are instructions appended after ALLOCO for constant attrs
    final List<LoadInstr> constInstrs = new ArrayList<>();

    /*
     * Make an argument vector to pass object attributes
     */
    instructions.add(new NoArgInstr(objectLiteral.start, objectLiteral.end, MAKEARGV));

    /*
     * New way of instiating object literals:
     * 
     * We essentially treat it as a function call. Key-value pairs are passed on the operand stack
     * using an ArgVector. We then use the the "allocos" instruction to properly setup this object
     * 
     * If the object literal is empty, we just use "alloco" for an empty object
     */
    for (Entry<String, Parameter> attr: objectLiteral.getAttributes().entrySet()) {
      final Parameter attrParam = attr.getValue();
      final StringConstant attrName = constantPool.addString(attr.getKey());

      final NodeResult valueRes = attrParam.getInitValue().accept(this, parentContext);
      if(valueRes.hasExceptions()) {
        exceptions.addAll(valueRes.getExceptions());
      }
      else {
        instructions.addAll(valueRes.getInstructions());
        instructions.add(attrName.linkInstr(new LoadInstr(attrParam.start, attrParam.end, ARG, attrName.getExactIndex())));
      }

      if (attr.getValue().hasDescriptor(TokenType.CONST)) {
        final IntegerConstant descriptor = constantPool.addInt(1);
        constInstrs.add(descriptor.linkInstr(new LoadInstr(attr.getValue().getName().start, 
                                                          attr.getValue().getName().end, 
                                                          LOADC, 
                                                          descriptor.getExactIndex())));
        constInstrs.add(attrName.linkInstr(new LoadInstr(attr.getValue().getName().start, 
                                                         attr.getValue().getName().end, 
                                                         MAKECONST, 
                                                         attrName.getExactIndex())));
      }
    }

    final int isSealed = objectLiteral.isSealed() ? 1 : 0;

    instructions.add(new ArgInstr(objectLiteral.start, objectLiteral.end, ALLOCO, isSealed));
    instructions.addAll(constInstrs);

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitCall(CompContext parentContext, FuncCall funcCall) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //System.out.println(" ===> visit call: "+funcCall.getTarget().repr()+" => "+Arrays.toString(funcCall.getArguments()));

    /*
     * Make an argument vector to pass function arguments
     */
    instructions.add(new NoArgInstr(funcCall.start, funcCall.end, MAKEARGV));

    for(Argument arg : funcCall.getArguments()) {
      arg.getArgument().accept(this, parentContext).pipeErr(exceptions).pipeInstr(instructions);
      /**
       * If the argument isn't geared towards an optional parameter,
       * the argNameIndex is -1, signaling that it's a positional argument.
       */
      if (arg.hasName()) {
        final StringConstant argName = constantPool.addString(arg.getParamName().getIdentifier());
        instructions.add(argName.linkInstr(new LoadInstr(arg.getParamName().start, 
                                                         arg.getArgument().end, 
                                                         ARG, 
                                                         argName.getExactIndex())));
      }
      else {
        instructions.add(new LoadInstr(arg.getArgument().start, 
                                      arg.getArgument().end, 
                                      ARG, 
                                      -1));
      }
    }

    funcCall.getTarget().accept(this, parentContext).pipeErr(exceptions).pipeInstr(instructions);
    instructions.add(new NoArgInstr(funcCall.start, funcCall.end, CALL));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitAttrAccess(CompContext parentContext, AttrAccess attrAccess) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final CompContext newContext = new CompContext(parentContext, parentContext.getCurrentContext());
    newContext.setContextValue(ContextKey.NEED_STORAGE, false);

    //Add instructions for target first
    final NodeResult targetResult = attrAccess.getTarget().accept(this, newContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }

    /*
     * Allocate attribute name in the constant pool. 
     */
    final StringConstant attrName = constantPool.addString(attrAccess.getAttrName().getIdentifier());
    if (parentContext.hasContextValue(ContextKey.NEED_STORAGE) && (boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(attrName.linkInstr(new StoreInstr(attrAccess.start, attrAccess.end, STOREATTR, attrName.getExactIndex())));
    }
    else {
      instructions.add(attrName.linkInstr(new LoadInstr(attrAccess.start, attrAccess.end, LOADATTR, attrName.getExactIndex())));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitArray(CompContext parentContext, ArrayLiteral arrayLiteral) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    /*
     * Make argument vector for array elements 
     */
    instructions.add(new NoArgInstr(arrayLiteral.start, arrayLiteral.end, MAKEARGV));

    /*
     * Pass each array value as an argument to the arg vector
     */
    for (Node value : arrayLiteral.getValues()) {
      final NodeResult valueResult = value.accept(this, parentContext);
      if (valueResult.hasExceptions()) {
        exceptions.addAll(valueResult.getExceptions());
      }
      else {
        instructions.addAll(valueResult.getInstructions());
        instructions.add(new LoadInstr(value.start, value.end, ARG, -1));
      }
    }

    /**
     * Allocate the array, given the argument vector
     */
    instructions.add(new NoArgInstr(arrayLiteral.start, arrayLiteral.end, ALLOCA));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitConstAttrDeclr(CompContext parentContext, ConstAttrDeclr constAttrDeclr) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final ConstantPool pool = parentContext.getConstantPool();

    //Compile object target
    constAttrDeclr.getAttr().getTarget()
                            .accept(this, parentContext)
                            .pipeErr(exceptions)
                            .pipeInstr(instrs);

    //Allocate attr name on constant pool
    final StringConstant attrName = pool.addString(constAttrDeclr.getAttr().getAttrName().getIdentifier());

    //Allocate int for modifier
    final IntegerConstant constCode = pool.addInt(1);
    instrs.add(constCode.linkInstr(new LoadInstr(constAttrDeclr.start, constAttrDeclr.end, LOADC, constCode.getExactIndex())));

    //Use the MAKECONST instr
    instrs.add(attrName.linkInstr(new LoadInstr(constAttrDeclr.start, constAttrDeclr.end, MAKECONST, attrName.getExactIndex())));

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  @Override
  public NodeResult visitIndexAccess(CompContext parentContext, IndexAccess arrayAccess) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final CompContext newContext = new CompContext(parentContext, parentContext.getCurrentContext());
    newContext.setContextValue(ContextKey.NEED_STORAGE, false);

    //Add instructions for target first
    arrayAccess.getTarget().accept(this, newContext).pipeErr(exceptions).pipeInstr(instructions);

    /*
     * Compile index expression
     */
    arrayAccess.getIndex().accept(this, newContext).pipeErr(exceptions).pipeInstr(instructions);

    /*
     * If this is an assignment/storage operation, use STOREATTR 
     */
    if (parentContext.hasContextValue(ContextKey.NEED_STORAGE) && (boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, STOREIN));
    }
    else {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, LOADIN));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public VarResult visitVarDeclrList(CompContext parentContext, VarDeclrList varDeclrList) {
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();
    final LinkedHashMap<Identifier, LoadStorePair> vars = new LinkedHashMap<>();

    for (VarDeclr var : varDeclrList.getVarDeclrs()) {
      final VarResult result = (VarResult) var.accept(this, parentContext);
      result.pipeErr(exceptions).pipeInstr(instrs);

      if (!result.hasExceptions()) {
        vars.putAll(result.getVars());
      }
    }
 
    return new VarResult(exceptions, vars, instrs);
  }

  /**
   * This method will generate its own load/store instructions based
   * on the nearest VarAllocator in its CompContext.
   */
  @Override
  public VarResult visitVarDeclr(CompContext parentContext, VarDeclr varDeclr) {
    final VarAllocator varAllocator = (VarAllocator) parentContext.getValue(ContextKey.VAR_ALLOCATOR);
    final LoadStorePair varLoadStore = varAllocator.generate(varDeclr.getName().getIdentifier(), varDeclr.start, varDeclr.end);

    final List<ValidationException> exceptions = new ArrayList<>();
    final List<Instruction> instrs = new ArrayList<>();
    
    if (varDeclr.hasInitialValue()) {
      final NodeResult result = varDeclr.getInitialValue().accept(this, parentContext);
      if (result.hasExceptions()) {
        return new VarResult(result.getExceptions(), new LinkedHashMap<>(), Collections.emptyList());
      }
      
      instrs.addAll(result.getInstructions());
    }
    else {
      //If variable has no initial value, then it's initial value is null by default.
      instrs.add(new NoArgInstr(varDeclr.end, varDeclr.end, LOADNULL));
    }

    //Store value in variable
    instrs.add(varLoadStore.store);

    if (Keyword.hasKeyword(TokenType.EXPORT, varDeclr.getDescriptors())) {

      /**
       * We only add special instructions for export/const
       * if the variable is a module/top-level variable.
       * 
       * We can determine this if the variable isn't within a function.
       */
      if (parentContext.isWithinContext(ContextType.FUNCTION)) {
        exceptions.add(new ValidationException("'"+varDeclr.getName().getIdentifier()+"' can't be exported as it's a local variable", 
                                                varDeclr.getName().start, 
                                                varDeclr.getName().end));
      }
      else {
        /*
        * If variable is exported, add EXPORTMV instruction
        * 
        * (only module/top-level variable can be exported)
        */
        instrs.add(new ArgInstr(varDeclr.getName().start, 
                                varDeclr.getName().end, 
                                EXPORTMV, 
                                varLoadStore.store.getIndex()));
      }
    }
    if (varDeclr.isConst()) {
      /*
      * If variable is constant, add CONSTMV instruction (if it's module/top-level)
      * 
      * If it's local, then there's no need to do anything.
      * 
      * (only module/top-level variable can be exported)
      */
      if (!parentContext.isWithinContext(ContextType.FUNCTION)) {
        instrs.add(new ArgInstr(varDeclr.getName().start, 
                              varDeclr.getName().end, 
                              CONSTMV, 
                              varLoadStore.store.getIndex()));
      }
    }
  
    LOG.info(" ==== '"+varDeclr.getName()+"' instr: "+instrs.stream().map(Instruction::toString).collect(Collectors.joining(System.lineSeparator())));
    return VarResult.single(varDeclr.getName(), varLoadStore, instrs);
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final Operator op = unaryExpr.getOperator();

    //Compile target expression first
    unaryExpr.getTarget().accept(this, parentContext).pipeErr(exceptions).pipeInstr(instructions);

    //Then add instruction for unary operator
    switch (op.getOp()) {
      case MINUS: {
        instructions.add(new NoArgInstr(unaryExpr.start, unaryExpr.end, NEG));
        break;
      }
      case BANG: {
        instructions.add(new NoArgInstr(unaryExpr.start, unaryExpr.end, NOT));
        break;
      }
      default: invalid(new ValidationException("'"+op.getOp().str+"' is an invalid unary operator.", 
                                               op.start, 
                                               op.end));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  //Utility methods - START
  private static long labelTag = 0;
  
  private static String genLabelName(String labelName) {
    String ret = labelName+labelTag;
    labelTag++;
    return ret;
  }

  private static Node unwrap(Node target) {
    while (target instanceof Parenthesized) {
      target = ((Parenthesized) target).getInner();
    }
    return target;
  }
  //Utility methods - END
}
