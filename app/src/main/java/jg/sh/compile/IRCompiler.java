package jg.sh.compile;

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

import jg.sh.common.Location;
import jg.sh.common.OperatorKind;
import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompContext.ContextType;
import jg.sh.compile.CompContext.IdentifierInfo;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.CommentInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.LoadStorePair;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.instrs.StoreCellInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.StringConstant;
import jg.sh.parsing.Module;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.ArrayLiteral;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
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
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.statements.CaptureStatement;
import jg.sh.parsing.nodes.statements.DataDefinition;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.UseStatement;
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

import static jg.sh.compile.NodeResult.*;
import static jg.sh.compile.instrs.OpCode.*;

public class IRCompiler implements NodeVisitor<NodeResult, CompContext> {

  public static class CompilerResult {
    private final ObjectFile objectFile;
    private final List<ValidationException> validationExceptions;

    public CompilerResult(ObjectFile objectFile) {
      this.objectFile = objectFile;
      this.validationExceptions = null;
    }

    public CompilerResult(List<ValidationException> validationExceptions) {
      this.validationExceptions = validationExceptions;
      this.objectFile = null;
    }

    public boolean isSuccessful() {
      return objectFile != null;
    }

    public ObjectFile getObjectFile() {
      return objectFile;
    }

    public List<ValidationException> getValidationExceptions() {
      return validationExceptions;
    }
  }

  /**
   * Utility class for passing information
   * about the load/store instructions of a variable.
   */
  private static class VarResult extends NodeResult {

    private final LinkedHashMap<Identifier, LoadStorePair> vars;

    private VarResult(List<ValidationException> exceptions, 
                      LinkedHashMap<Identifier, LoadStorePair> vars,
                      List<Instruction> instrs) {
      super(exceptions, instrs);  
      this.vars = vars;
    }

    public LinkedHashMap<Identifier, LoadStorePair> getVars() {
      return vars;
    }

    public static VarResult single(Identifier var, 
                                   LoadStorePair loadStore, 
                                   Instruction ... instrs) {
      LinkedHashMap<Identifier, LoadStorePair> map = new LinkedHashMap<>();
      map.put(var, loadStore);
      return new VarResult(Collections.emptyList(), map, Arrays.asList(instrs));
    }

    public static VarResult single(Identifier var, 
                                   LoadStorePair loadStore, 
                                   List<Instruction> instrs) {
      LinkedHashMap<Identifier, LoadStorePair> map = new LinkedHashMap<>();
      map.put(var, loadStore);
      return new VarResult(Collections.emptyList(), map, instrs);
    }
  }

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
      final int moduleNameIndex = constantPool.addComponent(new StringConstant(name));
      return new LoadStorePair(new LoadCellInstr(start, end, LOADMV, moduleNameIndex), 
                               new StoreCellInstr(start, end, STOREMV, moduleNameIndex));
    };
    moduleContext.setContextValue(ContextKey.VAR_ALLOCATOR, allocator);

    /*
     * Unbounded functions and toplevel statements, when referring to "self", means
     * they're referring to the module instance.
     */
    final LoadStorePair moduleLoadStore = new LoadStorePair(new LoadCellInstr(Location.DUMMY, Location.DUMMY, LOADMOD, -1), 
                                                            new StoreCellInstr(Location.DUMMY, Location.DUMMY, LOADMOD, -1));
    final Keyword constantKeyword = new Keyword(TokenType.CONST, Location.DUMMY, Location.DUMMY);
    
    moduleContext.addVariable(TokenType.SELF.name().toLowerCase(), 
                              moduleLoadStore,
                              constantKeyword);
    moduleContext.addVariable(TokenType.MODULE.name().toLowerCase(), 
                              moduleLoadStore,
                              constantKeyword);
    moduleContext.setContextValue(ContextKey.SELF_CODE, moduleLoadStore.load);

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

    final ArrayList<Statement> toCompile = new ArrayList<>();

    /**
     * Iterate through statements and for functions and data definitions,
     * generate load and store instructions, but don't compile their bodies.
     * 
     * For Variable declarations, do both.
     */
    for (Statement statement : module.getStatements()) {
      if(statement instanceof DataDefinition){
        final DataDefinition dataDef = (DataDefinition) statement;

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
          toCompile.add(dataDef);
          moduleContext.addVariable(dataDef.getName().getIdentifier(), dataDefModVar);
        }
      }
      else if(statement.getExpr() instanceof FuncDef) {
        final FuncDef func = (FuncDef) statement.getExpr();

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
          toCompile.add(statement);
          moduleContext.addVariable(func.getBoundName().getIdentifier(), funcDefModVar);
        }
      }
      else if(statement.getExpr() instanceof VarDeclr) {
        final VarDeclr varDeclr = (VarDeclr) statement.getExpr();


        final VarResult varResult = (VarResult) varDeclr.accept(this, moduleContext);

        if (takenTopLevelSymbols.contains(varDeclr.getName())) {
          exceptions.add(new ValidationException("'"+varDeclr.getName().getIdentifier()+"' is already a top-level symbol.", 
                                                 varDeclr.getName().start, 
                                                 varDeclr.getName().end));
        }
        else if(varResult.hasExceptions()) {
          exceptions.addAll(varResult.getExceptions());
        }
        else {
          takenTopLevelSymbols.add(varDeclr.getName());
          moduleContext.addVariable(varDeclr.getName().getIdentifier(), 
                                    varResult.getVars().get(varDeclr.getName()), 
                                    varDeclr.getDescriptors());
        }
      }
      else {
        final NodeResult result = statement.accept(this, moduleContext);
        if (result.hasExceptions()) {
          exceptions.addAll(result.getExceptions());
        }
        else {
          instrs.addAll(result.getInstructions());
        }
      }
    }
    

    final ObjectFile objectFile = new ObjectFile(module.getName(), moduleStart.getName(), constantPool, instrs);
    return exceptions.isEmpty() ? new CompilerResult(objectFile) : new CompilerResult(exceptions);
  }

  @Override
  public NodeResult visitStatement(CompContext parentContext, Statement statement) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitStatement'");
  }

  @Override
  public VarResult visitUseStatement(CompContext parentContext, UseStatement useStatement) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final Identifier moduleName = useStatement.getModuleName();
    final VarAllocator varAllocator = (VarAllocator) parentContext.getValue(ContextKey.VAR_ALLOCATOR);

    //Allocate constant string for module name
    final int moduleNameIndex = constantPool.addComponent(new StringConstant(moduleName.getIdentifier()));
    
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
                              new LoadCellInstr(useStatement.start,
                                                useStatement.end,
                                                LOADMOD, 
                                                moduleNameIndex),
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
        instrs.add(new LoadCellInstr(useStatement.start,
                                     useStatement.end,
                                     LOADMOD, 
                                     moduleNameIndex));

        //The variable name to refer to the imported module component
        final Identifier moduleVarHandle = compEntry.getValue() != null ?
                                            compEntry.getValue() : 
                                            compEntry.getKey();

        final int compNameIndex = constantPool.addComponent(new StringConstant(compEntry.getKey().getIdentifier()));

        //Load and store instructions for impoted module components
        final LoadStorePair loadStore = varAllocator.generate(moduleVarHandle.getIdentifier(), 
                                                              moduleVarHandle.start, 
                                                              moduleVarHandle.end);

        //Load the specific attribute so it can be stored in our module
        instrs.add(new LoadCellInstr(compEntry.getKey().start, 
                                     compEntry.getKey().end, 
                                     LOADATTR, 
                                     compNameIndex));
        
        instrs.add(loadStore.store);
        compMap.put(moduleVarHandle, loadStore);
      }

      return new VarResult(Collections.emptyList(), compMap, instrs);
    }
  }

  @Override
  public NodeResult visitReturnStatement(CompContext parentContext, ReturnStatement returnStatement) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitReturnStatement'");
  }

  @Override
  public NodeResult visitDataDefinition(CompContext parentContext, DataDefinition dataDefinition) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitDataDefinition'");
  }

  @Override
  public NodeResult visitCaptureStatement(CompContext parentContext, CaptureStatement captureStatement) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitCaptureStatement'");
  }

  @Override
  public NodeResult visitBlock(CompContext parentContext, Block block) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitBlock'");
  }

  @Override
  public NodeResult visitIfBlock(CompContext parentContext, IfBlock ifBlock) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitIfBlock'");
  }

  @Override
  public NodeResult visitTryCatchBlock(CompContext parentContext, TryCatch tryCatch) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitTryCatchBlock'");
  }

  @Override
  public NodeResult visitWhileBlock(CompContext parentContext, WhileBlock whileBlock) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitWhileBlock'");
  }

  @Override
  public NodeResult visitString(CompContext parentContext, Str str) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new StringConstant(str.getValue()));
    return valid(new ArgInstr(str.start, str.end, LOADC, index));
  }

  @Override
  public NodeResult visitInt(CompContext parentContext, Int integer) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new IntegerConstant(integer.getValue()));
    return valid(new ArgInstr(integer.start, integer.end, LOADC, index));
  }

  @Override
  public NodeResult visitBoolean(CompContext parentContext, Bool bool) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new BoolConstant(bool.getValue()));
    return valid(new ArgInstr(bool.start, bool.end, LOADC, index));
  }

  @Override
  public NodeResult visitFloat(CompContext parentContext, FloatingPoint floatingPoint) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new FloatConstant(floatingPoint.getValue()));
    return valid(new ArgInstr(floatingPoint.start, floatingPoint.end, LOADC, index));
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

    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
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
        return valid(new LoadCellInstr(keyword.start, keyword.end, LOADMOD, -1));
      }
      case SELF: {
        final IdentifierInfo info = parentContext.getVariable(TokenType.SELF.name().toLowerCase());

        if(info == null) {
          return invalid(new ValidationException("'self' is unfound.", keyword.start, keyword.end));
        }

        return valid(info.getLoadInstr());
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

  @Override
  public NodeResult visitFuncDef(CompContext parentContext, FuncDef funcDef) {
    final ConstantPool pool = parentContext.getConstantPool();
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    final CompContext funcContext = new CompContext(parentContext, ContextType.FUNCTION);

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
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

    if(op == Op.ASSIGNMENT) {
      /*
       * Compile value expression first
       */
      final NodeResult valResult = binaryOpExpr.getRight().accept(this, parentContext);
      if(valResult.hasExceptions()) {
        exceptions.addAll(valResult.getExceptions());
      }
      else {
        instrs.addAll(valResult.getInstructions());
      }

      final CompContext leftContext = new CompContext(parentContext, parentContext.getCurrentContext());
      leftContext.setContextValue(ContextKey.NEED_STORAGE, true);

      /*
       * Compile assignee next.
       */
      final NodeResult assigneeRes = binaryOpExpr.getRight().accept(this, leftContext);
      if(assigneeRes.hasExceptions()) {
        exceptions.addAll(assigneeRes.getExceptions());
      }
      else {
        instrs.addAll(assigneeRes.getInstructions());
      }
    }
    else if(Op.mutatesLeft(op)) {
      /*
       * Expand expression, from a += b to a = a * b
       * where * is any operator
       */

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
      return assignExpr.accept(this, parentContext);
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
      final int falseConstant = pool.addComponent(new BoolConstant(false));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant));

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
      final int falseConstant = pool.addComponent(new BoolConstant(false));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant));
      instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMP, endBranch));

      //operandTrue label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, operandTrue));
      final int trueConstantAddr = pool.addComponent(new BoolConstant(true));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, trueConstantAddr));

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
        instrs.add(new ArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, ARG, -1));
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
        instrs.add(new ArgInstr(binaryOpExpr.getRight().start, binaryOpExpr.getRight().end, ARG, -1));
      }
      
      final int systemModuleName = pool.addComponent(new StringConstant("system"));
      final int bindName = pool.addComponent(new StringConstant("bind"));
      
      /**
       * Call system.bind()
       */
      instrs.add(new LoadCellInstr(binaryOpExpr.start, binaryOpExpr.end, LOADMV, systemModuleName));
      instrs.add(new LoadCellInstr(binaryOpExpr.start, binaryOpExpr.end, LOADATTR, bindName));
      instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, CALL));
    }
    else {
      /**
       * Compile left operand first.
       */
      final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
      if (leftResult.hasExceptions()) {
        exceptions.addAll(leftResult.getExceptions());
      }
      else {
        instrs.addAll(leftResult.getInstructions());
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
      }

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
      final int attrNameIndex = constantPool.addComponent(new StringConstant(attr.getKey()));

      final NodeResult valueRes = attrParam.getInitValue().accept(this, parentContext);
      if(valueRes.hasExceptions()) {
        exceptions.addAll(valueRes.getExceptions());
      }
      else {
        instructions.addAll(valueRes.getInstructions());
        instructions.add(new ArgInstr(attrParam.start, attrParam.end, ARG, attrNameIndex));
      }
    }

    instructions.add(new NoArgInstr(objectLiteral.start, objectLiteral.end, ALLOCO));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitCall(CompContext parentContext, FuncCall funcCall) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    /*
     * Make an argument vector to pass function arguments
     */
    instructions.add(new NoArgInstr(funcCall.start, funcCall.end, MAKEARGV));

    for(Argument arg : funcCall.getArguments()) {
      final NodeResult result = arg.getArgument().accept(this, parentContext);

      if (result.hasExceptions()) {
        exceptions.addAll(result.getExceptions());
      }
      else {
        /**
         * If the argument isn't geared towards an optional parameter,
         * the argNameIndex is -1, signaling that it's a positional argument.
         */
        final int argNameIndex = arg.hasName() ? 
                                    constantPool.addComponent(new StringConstant(arg.getParamName().getIdentifier())) : 
                                    -1;

        instructions.add(new ArgInstr(arg.getArgument().start, 
                                      arg.getArgument().end, 
                                      ARG, 
                                      argNameIndex));
      }
    }

    final NodeResult targetResult = funcCall.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
      instructions.add(new NoArgInstr(funcCall.start, funcCall.end, CALL));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitAttrAccess(CompContext parentContext, AttrAccess attrAccess) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Add instructions for target first
    final NodeResult targetResult = attrAccess.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }

    /*
     * Allocate attribute name in the constant pool. 
     */
    final int attrNameIndex = constantPool.addComponent(new StringConstant(attrAccess.getAttrName().getIdentifier()));
    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(new StoreCellInstr(attrAccess.start, attrAccess.end, STOREATTR, attrNameIndex));
    }
    else {
      instructions.add(new LoadCellInstr(attrAccess.start, attrAccess.end, LOADATTR, attrNameIndex));
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
        instructions.add(new ArgInstr(value.start, value.end, ARG, -1));
      }
    }

    /**
     * Allocate the array, given the argument vector
     */
    instructions.add(new NoArgInstr(arrayLiteral.start, arrayLiteral.end, ALLOCA));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitIndexAccess(CompContext parentContext, IndexAccess arrayAccess) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Add instructions for target first
    final NodeResult targetResult = arrayAccess.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }

    /*
     * Compile index expression
     */
    final NodeResult indexResult = arrayAccess.getIndex().accept(this, parentContext);
    if (indexResult.hasExceptions()) {
      exceptions.addAll(indexResult.getExceptions());
    }
    else {
      instructions.addAll(indexResult.getInstructions());
    }

    /*
     * If this is an assignment/storage operation, use STOREATTR 
     */
    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, STOREIN));
    }
    else {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, LOADIN));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public VarResult visitVarDeclr(CompContext parentContext, VarDeclr varDeclr) {
    final VarAllocator varAllocator = (VarAllocator) parentContext.getValue(ContextKey.VAR_ALLOCATOR);
    final LoadStorePair varLoadStore = varAllocator.generate(varDeclr.getName().getIdentifier(), varDeclr.start, varDeclr.end);

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

    return VarResult.single(varDeclr.getName(), varLoadStore, instrs);
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Compile target expression first
    final NodeResult targetResult = unaryExpr.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }


    //Then add instruction for unary operator
    final Operator op = unaryExpr.getOperator();
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

  private static <T> List<T> concat(List<T> left, List<T> right) {
    final ArrayList<T> newList = new ArrayList<>(left);
    newList.addAll(right);
    return newList;
  }
  //Utility methods - END
}
