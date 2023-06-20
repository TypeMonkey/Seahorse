package jg.sh.irgen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import jg.sh.common.OperatorKind;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.BinaryOpExpr;
import jg.sh.compile.parsing.nodes.CallArg;
import jg.sh.compile.parsing.nodes.FunctionCall;
import jg.sh.compile.parsing.nodes.ReservedWords;
import jg.sh.compile.parsing.nodes.atoms.AttrAccess;
import jg.sh.compile.parsing.nodes.atoms.ObjectLiteral;
import jg.sh.compile.parsing.nodes.atoms.FuncDef;
import jg.sh.compile.parsing.nodes.atoms.Identifier;
import jg.sh.compile.parsing.nodes.atoms.NullValue;
import jg.sh.compile.parsing.nodes.atoms.Parameter;
import jg.sh.compile.parsing.nodes.atoms.Parenthesized;
import jg.sh.compile.parsing.nodes.atoms.Unary;
import jg.sh.compile.parsing.nodes.atoms.constants.Bool;
import jg.sh.compile.parsing.nodes.atoms.constants.FloatingPoint;
import jg.sh.compile.parsing.nodes.atoms.constants.Int;
import jg.sh.compile.parsing.nodes.atoms.constants.Str;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.ArrayAccess;
import jg.sh.compile.parsing.nodes.atoms.ArrayLiteral;
import jg.sh.compile.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.Block;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.IfElse;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.ScopeBlock;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.TryCatch;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.WhileLoop;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.ExpressionStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.KeywordStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.UseStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.VariableStatement;
import jg.sh.irgen.ContextManager.ContextKey;
import jg.sh.irgen.ContextManager.ContextType;
import jg.sh.irgen.ContextManager.IdentifierInfo;
import jg.sh.irgen.instrs.ArgInstr;
import jg.sh.irgen.instrs.CommentInstr;
import jg.sh.irgen.instrs.Instruction;
import jg.sh.irgen.instrs.JumpInstr;
import jg.sh.irgen.instrs.LabelInstr;
import jg.sh.irgen.instrs.LoadCellInstr;
import jg.sh.irgen.instrs.NoArgInstr;
import jg.sh.irgen.instrs.OpCode;
import jg.sh.irgen.instrs.StoreCellInstr;
import jg.sh.irgen.pool.ConstantPool;
import jg.sh.irgen.pool.component.BoolConstant;
import jg.sh.irgen.pool.component.CodeObject;
import jg.sh.irgen.pool.component.ErrorHandlingRecord;
import jg.sh.irgen.pool.component.FloatConstant;
import jg.sh.irgen.pool.component.IntegerConstant;
import jg.sh.irgen.pool.component.StringConstant;

/**
 * Houses logic for the compilation
 * of SeaHorse programs into their SeaHorse bytecode equivalent
 * @author Jose
 *
 */
public class IRCompiler {
  
  public IRCompiler() {}
  
  public CompiledFile[] compileModules(Module ... module) {
    CompiledFile [] compiledFiles = new CompiledFile[module.length];
    
    for (int i = 0; i < module.length; i++) {
      compiledFiles[i] = compileModule(module[i]);
    }
    
    return compiledFiles;
  }
  
  private CompiledFile compileModule(Module module) {
    ArrayList<Instruction> moduleInstructions = new ArrayList<>();
    
    moduleInstructions.add(new CommentInstr(" START OF MODULE IMPORTS!!"));
    
    //Create a label to signify the start of module instructions
    final String moduleStartLabel = genLabelName("moduleStart");
    
    moduleInstructions.add(new LabelInstr(0, 0, moduleStartLabel));
    
    final ConstantPool constantPool = new ConstantPool();
    
    //Context Manager for the whole module
    final ContextManager moduleContext = new ContextManager(ContextType.MODULE);
    
    //Add the self keyword, but the actual instruction is just loading the current module
    moduleContext.addVariable(ReservedWords.SELF.actualWord, 
                              new LoadCellInstr(-1, -1, OpCode.LOADMOD, -1), 
                              new StoreCellInstr(-1, -1, OpCode.LOADMOD, -1));
    
    final Instruction [] moduleSelfCode = {new LoadCellInstr(-1, -1, OpCode.LOADMOD, -1)};
    moduleContext.setContextValue(ContextKey.SELF_CODE, moduleSelfCode);
    
    //Compile the use statements
    for(UseStatement use : module.getImports()) {
      
      final int moduleNameIndex = constantPool.addComponent(new StringConstant(use.getTargetModule()));
      
      if (use.getComponents().size() == 0) {
        //Bare module import. No components to import specifically
        
        moduleInstructions.add(new CommentInstr("Bare loading module '"+use.getTargetModule()+"'"));
        
        /*
         * These are the default load/store instructions for module variables
         */
        final LoadCellInstr loadInstr = new LoadCellInstr(use.getLine(),  
            use.getColumn(), 
            OpCode.LOADMV, 
            moduleNameIndex);
        final StoreCellInstr storeInstr = new StoreCellInstr(use.getLine(),  
            use.getColumn(), 
            OpCode.STOREMV, 
            moduleNameIndex);
        
        //First, load the module
        moduleInstructions.add(new LoadCellInstr(use.getLine(), use.getColumn(), OpCode.LOADMOD, moduleNameIndex));
        
        //second, store the loaded module in the corresponding module variable
        moduleContext.addVariable(use.getTargetModule(), loadInstr, storeInstr);
        moduleInstructions.add(storeInstr);
      }
      else {
        //Module import is specific to these names
        
        for (String componentName : use.getComponents()) {
          
          moduleInstructions.add(new CommentInstr("Loading the '"+componentName+"' attribute from module '"+use.getTargetModule()+"'"));
          
          //first, load the module
          moduleInstructions.add(new LoadCellInstr(use.getLine(), use.getColumn(), OpCode.LOADMOD, moduleNameIndex));
          
          //second, allocate the string literal for the componentName
          int compNameIndex = constantPool.addComponent(new StringConstant(componentName));
          moduleInstructions.add(new LoadCellInstr(use.getLine(), use.getColumn(), OpCode.LOADATTR, compNameIndex));
          
          /*
           * These are the default load/store instructions for module component variables
           */
          final LoadCellInstr loadInstr = new LoadCellInstr(use.getLine(),  
              use.getColumn(), 
              OpCode.LOADMV, 
              compNameIndex);
          final StoreCellInstr storeInstr = new StoreCellInstr(use.getLine(),  
              use.getColumn(), 
              OpCode.STOREMV, 
              compNameIndex);
          
          //third, store the component value in the corresponding module variable
          moduleContext.addVariable(componentName, loadInstr, storeInstr);
          moduleInstructions.add(storeInstr);
        }
      }
    }
    
    moduleInstructions.add(new CommentInstr(" Instructions for defining module variables "));
    
    //Add module setting and retrieval instructions first
    module.getStatements().stream()
                          .filter(s -> s instanceof VariableStatement)
                          .forEach(s -> {
                            VariableStatement moduleVarStatement = (VariableStatement) s;
                            
                            /*
                             * These are the default load/store instructions for module variables
                             */
                            final int moduleVarNameIndex = constantPool.addComponent(new StringConstant(moduleVarStatement.getName()));
                            
                            final LoadCellInstr loadInstr = new LoadCellInstr(moduleVarStatement.getLine(),  
                                moduleVarStatement.getColumn(), 
                                OpCode.LOADMV, 
                                moduleVarNameIndex);
                            final StoreCellInstr storeInstr = new StoreCellInstr(moduleVarStatement.getLine(),  
                                moduleVarStatement.getColumn(), 
                                OpCode.STOREMV, 
                                moduleVarNameIndex);

                            moduleContext.addVariable(moduleVarStatement.getName(), loadInstr, storeInstr);
                          });
    
    moduleInstructions.add(new CommentInstr(" START OF MODULE STATEMENTS!!"));
    
    //now compile the module statements
    for(Statement statement : module.getStatements()) {            
      //System.out.println("---> module statement: "+statement);
      
      //Just for debugging purposes
      moduleInstructions.add(new CommentInstr("For line #"+statement.getLine()));
      
      if (!(statement instanceof VariableStatement))  {
        moduleInstructions.addAll(compileStatement(statement, constantPool, moduleContext));
      }
      else {
        VariableStatement moduleVarStatement = (VariableStatement) statement;
        IdentifierInfo info = moduleContext.getVariable(moduleVarStatement.getName());
        
        moduleInstructions.add(new CommentInstr(" --- Definition for '"+moduleVarStatement.getName()+"'"));
        List<Instruction> varInstructions = compileVariableStatement(moduleVarStatement, constantPool, moduleContext, info.getStoreInstr());
        moduleInstructions.addAll(varInstructions);  
      }
    }
    
    return new CompiledFile(module.getName(), moduleStartLabel, constantPool, moduleInstructions);
  }
  
  private List<Instruction> compileStatement(Statement statement, 
                                             ConstantPool constantPool, 
                                             ContextManager contextManager){
    if (statement instanceof Block) {
      return compileBlock((Block) statement, constantPool, contextManager);
    }
    else if (statement instanceof ExpressionStatement) {
      ExpressionStatement exprStatement = (ExpressionStatement) statement;
      
      List<Instruction> exprInstructions = new ArrayList<>(compileExpr(exprStatement.getExpr(), constantPool, contextManager, false));
      
      if (exprStatement.getLeadingKeyword() == ReservedWords.THROW) {
        exprInstructions.add(new NoArgInstr(exprStatement.getLine(), exprStatement.getColumn(), OpCode.RETE));
      }
      else if (exprStatement.getLeadingKeyword() == ReservedWords.RETURN) {
        exprInstructions.add(new NoArgInstr(exprStatement.getLine(), exprStatement.getColumn(), OpCode.RET));
      }
      
      return exprInstructions;
    }
    else if (statement instanceof KeywordStatement) {
      KeywordStatement keywordStatement = (KeywordStatement) statement;
      
      if (keywordStatement.getReservedWord() == ReservedWords.RETURN) {
        return Arrays.asList(new NoArgInstr(keywordStatement.getLine(), keywordStatement.getColumn(), OpCode.RET));
      }
      else if (keywordStatement.getReservedWord() == ReservedWords.BREAK) {
        String loopBreakLabel = (String) contextManager.getValue(ContextKey.BREAK_LOOP_LABEL);
        return Arrays.asList(new JumpInstr(keywordStatement.getLine(), keywordStatement.getColumn(), OpCode.JUMP, loopBreakLabel));
      }
      else if (keywordStatement.getReservedWord() == ReservedWords.CONTINUE) {
        String loopContLabel = (String) contextManager.getValue(ContextKey.CONT_LOOP_LABEL);
        return Arrays.asList(new JumpInstr(keywordStatement.getLine(), keywordStatement.getColumn(), OpCode.JUMP, loopContLabel));
      }
    }
    else if (statement instanceof VariableStatement) {
      VariableStatement var = (VariableStatement) statement;
      
      StoreCellInstr storeCellInstr = null;
      LoadCellInstr loadCellInstr = null;
      
      if (contextManager.isWithinContext(ContextType.FUNCTION)) {
        //Get local var index
        final int varIndex = (int) contextManager.getValue(ContextKey.LOCAL_VAR_INDEX);
        
        //Update local var index
        contextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, varIndex + 1);
        
        storeCellInstr = new StoreCellInstr(var.getLine(), var.getColumn(), OpCode.STORE, varIndex);
        loadCellInstr = new LoadCellInstr(var.getLine(), var.getColumn(), OpCode.LOAD, varIndex);
      }
      else {
        final int nameIndex = constantPool.addComponent(new StringConstant(var.getName()));
        
        storeCellInstr = new StoreCellInstr(var.getLine(), var.getColumn(), OpCode.STOREMV, nameIndex);
        loadCellInstr = new LoadCellInstr(var.getLine(), var.getColumn(), OpCode.LOADMV, nameIndex);
      }
      
      List<Instruction> varInstrs = new ArrayList<>(compileVariableStatement(var, constantPool, contextManager, storeCellInstr));
      
      //Add EXPORTMV or CONSTMV for exported/constant module variables
      if(!contextManager.isWithinContext(ContextType.FUNCTION)) {
        //This is a module variable
        final Set<ReservedWords> varModifiers = Keyword.toReservedWords(var.getModifiers());
        if (varModifiers.contains(ReservedWords.CONST)) {
          varInstrs.add(new ArgInstr(var.getLine(), var.getColumn(), OpCode.CONSTMV, storeCellInstr.getIndex()));
        }
        
        if (varModifiers.contains(ReservedWords.EXPORT)) {
          varInstrs.add(new ArgInstr(var.getLine(), var.getColumn(), OpCode.EXPORTMV, storeCellInstr.getIndex()));
        }
      }
      
      contextManager.addVariable(var.getName(), loadCellInstr, storeCellInstr);
  
      return varInstrs;
    }
    
    throw new IllegalArgumentException("Unknown statement type: "+statement.getClass());
  }

  private List<Instruction> compileVariableStatement(VariableStatement variableStatement, 
                                                     ConstantPool constantPool, 
                                                     ContextManager contextManager,
                                                     StoreCellInstr storeInstr){
    ArrayList<Instruction> moduleInstructions = new ArrayList<>();
    
    List<Instruction> valueInstrs = compileExpr(variableStatement.getAssgnedExpr(), constantPool, contextManager, false);
    moduleInstructions.addAll(valueInstrs);
    moduleInstructions.add(storeInstr);
    return moduleInstructions;
  }
  
  private List<Instruction> compileBlock(Block block, ConstantPool constantPool, ContextManager contextManager){
    ArrayList<Instruction> blockIntrs = new ArrayList<>();
    
    if (block instanceof IfElse) {
      IfElse ifElse = (IfElse) block;
      
      //The label each branch jumps to upon completion
      final String doneLabel = genLabelName("done");
      
      //compile the branch code
      final String initBranch = genLabelName("initBranch");
      blockIntrs.add(new LabelInstr(ifElse.getLine(), ifElse.getColumn(), initBranch));
      blockIntrs.addAll(compileExpr(ifElse.getCond(), constantPool, contextManager, false));
      
      //Make new ContextManager for IfElse
      final ContextManager firstIfContext = new ContextManager(contextManager, ContextType.BLOCK);
      
      //If there's no other branches, just jump to the done label
      if(ifElse.getOtherBranches().isEmpty()) {
        blockIntrs.add(new JumpInstr(ifElse.getLine(), ifElse.getColumn(), OpCode.JUMPF, doneLabel));
        ifElse.getBranchCode().forEach(x -> blockIntrs.addAll(compileStatement(x, constantPool, firstIfContext)));
      }
      else {
        //Label for the next branch
        String nextLabel = genLabelName("branch");
        
        //if Condition is false, jump to the next branch's label
        blockIntrs.add(new JumpInstr(ifElse.getLine(), ifElse.getColumn(), OpCode.JUMPF, nextLabel));   
        
        blockIntrs.add(new CommentInstr(ifElse.getLine(), ifElse.getColumn(), "For line #"+ifElse.getLine()));
        
        ifElse.getBranchCode().forEach(x -> {
          blockIntrs.add(new CommentInstr(x.getLine(), x.getColumn(), "For line #"+x.getLine()));
          blockIntrs.addAll(compileStatement(x, constantPool, firstIfContext));
        });
        blockIntrs.add(new JumpInstr(ifElse.getLine(), ifElse.getColumn(), OpCode.JUMP, doneLabel));
        blockIntrs.add(new CommentInstr("END OF INIT IF"));
        
        for (int i = 0; i < ifElse.getOtherBranches().size(); i++) {
          final IfElse currentBranch = ifElse.getOtherBranches().get(i);
          //Add branch label first
          blockIntrs.add(new LabelInstr(currentBranch.getLine(), currentBranch.getColumn(), nextLabel));
          
          //generate label name for the next branch
          nextLabel = genLabelName("branch");
          
          //Make new ContextManager for other IfElse
          final ContextManager otherBranchContext = new ContextManager(contextManager, ContextType.BLOCK);
          
          //This is an elif block
          if(currentBranch.getCond() != null) {
            blockIntrs.addAll(compileExpr(currentBranch.getCond(), constantPool, otherBranchContext, false));
            
            //If this is the last elif block, then jump to done. Else, jump to the next elif/else label
            String nextLabelToJump = i == ifElse.getOtherBranches().size() - 1 ? doneLabel : nextLabel;
            blockIntrs.add(new JumpInstr(currentBranch.getLine(), currentBranch.getColumn(), OpCode.JUMPF, nextLabelToJump));
          }
                   
          currentBranch.getStatements().forEach(x -> {
            blockIntrs.add(new CommentInstr(x.getLine(), x.getColumn(), "For line #"+x.getLine()));
            blockIntrs.addAll(compileStatement(x, constantPool, otherBranchContext));
          });
          
          //If this is an elif block, jump to the done label after this branch is done executing.
          if(currentBranch.getCond() != null) {
            blockIntrs.add(new JumpInstr(currentBranch.getLine(), currentBranch.getColumn(), OpCode.JUMP, doneLabel));
          }
        }
      }
      
      //Add done labal
      blockIntrs.add(new LabelInstr(ifElse.getLine(), ifElse.getColumn(), doneLabel));
    }
    else if (block instanceof ScopeBlock) {
      ScopeBlock scopeBlock = (ScopeBlock) block;
      
      //Make new ContextManager for ScopeBlock
      final ContextManager scopeBlockManager = new ContextManager(contextManager, ContextType.BLOCK);
      
      scopeBlock.getStatements().forEach(x -> blockIntrs.addAll(compileStatement(x, constantPool, scopeBlockManager)));
    }
    else if (block instanceof TryCatch) {
      TryCatch tryCatch = (TryCatch) block;
      
      final String tryStartLabel = genLabelName("tryStart");
      final String tryEndLabel = genLabelName("tryEnd");
      final String catchLabel = genLabelName("catch");
      
      //Make new ContextManager for TryCatch - notably, the try-section
      final ContextManager tryContextManager = new ContextManager(contextManager, ContextType.BLOCK);
      
      constantPool.addComponent(new ErrorHandlingRecord(tryStartLabel, tryEndLabel, catchLabel));
      
      //add instructions for try block first
      blockIntrs.add(new LabelInstr(tryCatch.getLine(), tryCatch.getColumn(), tryStartLabel));
      tryCatch.getTargetBlock().forEach(x -> blockIntrs.addAll(compileStatement(x, constantPool, tryContextManager)));
      blockIntrs.add(new LabelInstr(tryCatch.getLine(), tryCatch.getColumn(), tryEndLabel));

      //Compile the catch part
      //Make new ContextManager for TryCatch - notably, the try-section
      final ContextManager catchContextManager = new ContextManager(contextManager, ContextType.BLOCK);
      
      //Add label for catch block
      blockIntrs.add(new LabelInstr(tryCatch.getLine(), tryCatch.getColumn(), catchLabel));
      
      final Identifier errorIdentifier = tryCatch.getErrorVar();
      
      StoreCellInstr errStore = null;
      LoadCellInstr errLoad = null;
      
      if (contextManager.isWithinContext(ContextType.FUNCTION)) {
        //Get local var index
        final int varIndex = (int) contextManager.getValue(ContextKey.LOCAL_VAR_INDEX);
        
        //Update local var index
        contextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, varIndex + 1);
        
        errStore = new StoreCellInstr(errorIdentifier.getLine(), errorIdentifier.getColumn(), OpCode.STORE, varIndex);
        errLoad = new LoadCellInstr(errorIdentifier.getLine(), errorIdentifier.getColumn(), OpCode.LOAD, varIndex);
      }
      else {
        final int nameIndex = constantPool.addComponent(new StringConstant(errorIdentifier.getIdentifier()));
        
        errStore = new StoreCellInstr(errorIdentifier.getLine(), errorIdentifier.getColumn(), OpCode.STOREMV, nameIndex);
        errLoad = new LoadCellInstr(errorIdentifier.getLine(), errorIdentifier.getColumn(), OpCode.LOADMV, nameIndex);
      }
      
      //Add instructions for storing the error object
      blockIntrs.add(new NoArgInstr(errorIdentifier.getLine(), errorIdentifier.getColumn(), OpCode.POPERR));
      blockIntrs.add(errStore);
      
      //add error variable to the context manager
      catchContextManager.addVariable(errorIdentifier.getIdentifier(), errLoad, errStore);
      
      tryCatch.getHandleBlock().forEach(x -> blockIntrs.addAll(compileStatement(x, constantPool, catchContextManager)));
    }
    else if (block instanceof WhileLoop) {
      WhileLoop whileLoop = (WhileLoop) block;
      
      final String loopLabel = genLabelName("while_loop");
      final String endLabel = genLabelName("loop_end");
      
      //Loop context
      final ContextManager loopContext = new ContextManager(contextManager, ContextType.LOOP);
      loopContext.setContextValue(ContextKey.CONT_LOOP_LABEL, loopLabel);
      loopContext.setContextValue(ContextKey.BREAK_LOOP_LABEL, endLabel);
      
      //Create label for while loop
      blockIntrs.add(new LabelInstr(whileLoop.getLine(), whileLoop.getColumn(), loopLabel));
      
      //Add instructions for loop condition first
      blockIntrs.addAll(compileExpr(whileLoop.getCondition(), constantPool, contextManager, false));
      
      blockIntrs.add(new JumpInstr(whileLoop.getLine(), whileLoop.getColumn(), OpCode.JUMPF, endLabel));
      
      whileLoop.getStatements().forEach(x -> blockIntrs.addAll(compileStatement(x, constantPool, loopContext)));
      
      blockIntrs.add(new JumpInstr(whileLoop.getLine(), whileLoop.getColumn(), OpCode.JUMP, loopLabel));
      
      blockIntrs.add(new LabelInstr(whileLoop.getLine(), whileLoop.getColumn(), endLabel));
    }
    
    return blockIntrs;
  }
  
  /*
  private List<Instruction> compileDataDefinition(DataDefinition def, ConstantPool constantPool, ContextManager manager){
    ArrayList<Instruction> dataDefInstrs = new ArrayList<>();
    
    ContextManager dataContextManager = new ContextManager(manager, ContextType.CLASS);
    
    ArrayList<Instruction> bindingInstructions = new ArrayList<>();
        
    //Compile instance functions
    for(Entry<String, FuncDef> insFunc : def.getMethods().entrySet()) {
      final int attrNameIndex = constantPool.addComponent(new StringConstant(insFunc.getKey()));
      
      bindingInstructions.addAll(compileFunction(insFunc.getValue(), constantPool, dataContextManager));
      bindingInstructions.add(new ArgInstr(insFunc.getValue().getLine(), insFunc.getValue().getColumn(), OpCode.STOREATTR, attrNameIndex));
    }
    
    //Allocate the name for this data definition
    final int nameIndex = constantPool.addComponent(new StringConstant(def.getName()));
    
    //Set typecode
    final int typeCode = (int) manager.getValue(ContextKey.TYPE_CODE);
    
    final ClassConstant dataDefConstant = new ClassConstant(nameIndex, typeCode, bindingInstructions);
    
    //Increment typecode
    manager.setContextValue(ContextKey.TYPE_CODE, typeCode + 1);
    
    final int dataDefConstantIndex = constantPool.addComponent(dataDefConstant);

    dataDefInstrs.add(new ArgInstr(def.getLine(), def.getColumn(), OpCode.LOADC, dataDefConstantIndex));
    return dataDefInstrs;
  }
  */
  
  private List<Instruction> compileFunction(FuncDef funcDef, ConstantPool constantPool, ContextManager contextManager){
    ArrayList<Instruction> funcInstructions = new ArrayList<>();
    
    //Create function level context-manager
    ContextManager funcContextManager = new ContextManager(contextManager, ContextType.FUNCTION);
        
    //Set the closure index
    funcContextManager.setContextValue(ContextKey.CL_VAR_INDEX, 0);
    
    //index for captured variables
    int captureIndex = 0;
    
    //int array for captured variables
    final int [] captures = new int[funcDef.getCaptures().size()];
        
    //Compile captures
    for(VariableStatement capture : funcDef.getCaptures()) {
      IdentifierInfo identifierInfo = funcContextManager.getVariable(capture.getName());
      if (identifierInfo == null) {
        throw new IllegalArgumentException("Cannot find captured variable '"+capture.getName()+"'");
      }
      
      /*
       * If the identifier is a local variable (not module-level), change the load instruction 
       * to be a LOAD/STORE_CL. 
       */
      if (identifierInfo.getContext().getCurrentContext() != ContextType.MODULE) {
        
        if(identifierInfo.getStoreInstr().getOpCode() != OpCode.STORE_CL && identifierInfo.getLoadInstr().getOpCode() != OpCode.LOAD_CL) {
          identifierInfo.getStoreInstr().setOpCode(OpCode.STORE_CL);
          identifierInfo.getLoadInstr().setOpCode(OpCode.LOAD_CL);
          
          System.out.println(" ---> "+identifierInfo.getContext().getContextMaps() +" || "+identifierInfo.getContext().getCurrentContext());
          
          final int closureIndex = (int) identifierInfo.getContext().getValue(ContextKey.CL_VAR_INDEX);
          
          identifierInfo.getLoadInstr().setIndex(closureIndex);
          identifierInfo.getStoreInstr().setIndex(closureIndex);

          identifierInfo.getContext().setContextValue(ContextKey.CL_VAR_INDEX, closureIndex + 1);
        }
      }
      
      LoadCellInstr loadInstr = new LoadCellInstr(capture.getLine(), capture.getColumn(), OpCode.LOAD_CL, captureIndex);
      StoreCellInstr storeInstr = new StoreCellInstr(capture.getLine(), capture.getColumn(), OpCode.STORE_CL, captureIndex);
      
      funcContextManager.addVariable(capture.getName(), loadInstr, storeInstr);
      
      //funcInstructions.add(new CaptureInstr(capture.getLine(), capture.getColumn(), identifierInfo.getLoadInstr().getIndex(), captureIndex));
      
      captures[captureIndex] = identifierInfo.getLoadInstr().getIndex(); 
      captureIndex++;
    }
    
    //Sets the starting index for local variables
    funcContextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, 0);
    
    /*
     * The first local variable - at index 0 - is the function itself
     */
    {
      /*
       * If this function has a bound name, recursion is possible!
       */
      
      final String funcBoundName = funcDef.hasName() ? funcDef.getBoundName() : "$recurse";
      
      final int recurseIndex = (int) funcContextManager.getValue(ContextKey.LOCAL_VAR_INDEX);
      
      LoadCellInstr loadRecurseFuncInstr = new LoadCellInstr(-1, -1, OpCode.LOAD, recurseIndex);
      StoreCellInstr storeRecurseFuncInstr = new StoreCellInstr(-1, -1, OpCode.STORE, recurseIndex);
      
      funcContextManager.addVariable(funcBoundName, loadRecurseFuncInstr, storeRecurseFuncInstr);
      
      funcContextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, recurseIndex + 1);
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
      final int selfIndex = (int) funcContextManager.getValue(ContextKey.LOCAL_VAR_INDEX);
      
      LoadCellInstr loadRecurseFuncInstr = new LoadCellInstr(-1, -1, OpCode.LOAD, selfIndex);
      StoreCellInstr storeRecurseFuncInstr = new StoreCellInstr(-1, -1, OpCode.STORE, selfIndex);
      
      funcContextManager.addVariable(ReservedWords.SELF.actualWord, loadRecurseFuncInstr, storeRecurseFuncInstr);
      
      funcContextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, selfIndex + 1);
    }
        
    //Generate unique label for function
    final String funcLabel = genLabelName(funcDef.hasName() ? funcDef.getBoundName()+"_" : "anonFunc_");
    LabelInstr labelInstr = new LabelInstr(funcDef.getLine(), funcDef.getColumn(), funcLabel);
    funcInstructions.add(labelInstr);
    
    /*
     * Keeps track of the local var index of keyword parameters. 
     * 
     * This is needed when the function is being called and the caller is preparing the
     * callee's parameter values.
     */
    HashMap<String, Integer> keywordParamToIndexMap = new HashMap<>();
    
    //Compile parameters
    for(Parameter parameter : funcDef.getParams().values()) {
      
      final int localVarOffset = (int) funcContextManager.getValue(ContextKey.LOCAL_VAR_INDEX);
      
      LoadCellInstr paramLoadInstr = new LoadCellInstr(parameter.getLine(), parameter.getColumn(), OpCode.LOAD, localVarOffset);
      StoreCellInstr paramStoreInstr = new StoreCellInstr(parameter.getLine(), parameter.getColumn(), OpCode.STORE, localVarOffset);
      
      if (parameter.isAKeywordParameter()) {
        List<Instruction> valueExpr = compileExpr(parameter.getInitValue(), constantPool, funcContextManager, false);
        funcInstructions.addAll(valueExpr);
        funcInstructions.add(paramStoreInstr);
        
        keywordParamToIndexMap.put(parameter.getName(), localVarOffset);
      }
      
      funcContextManager.addVariable(parameter.getName(), paramLoadInstr, paramStoreInstr);    
      
      funcContextManager.setContextValue(ContextKey.LOCAL_VAR_INDEX, localVarOffset + 1);
    }
    
    //Compile function body
    for(Statement statement : funcDef.getStatements()) {
      funcInstructions.add(new CommentInstr(statement.getLine(), statement.getColumn(), "For line #"+statement.getLine()));
      funcInstructions.addAll(compileStatement(statement, constantPool, funcContextManager));
    }
    
    //We'll append a "return null" at the end of the function as a catch-all for all branches
    funcInstructions.add(new CommentInstr(" <- catch all for all function branches ->"));
    funcInstructions.add(new NoArgInstr(-1, -1, OpCode.LOADNULL));
    funcInstructions.add(new NoArgInstr(-1, -1, OpCode.RET));
    
    //Now, allocate this function as a code object instance
    CodeObject funcObj = new CodeObject(funcDef.getSignature(), funcLabel, keywordParamToIndexMap, funcInstructions, captures);
    
    final int funcObjIndex = constantPool.addComponent(funcObj);
    
    //Actual function allocation code
    final Instruction [] selfLoadCode = (Instruction[]) contextManager.getValue(ContextKey.SELF_CODE);
    
    ArrayList<Instruction> actualInstrs = new ArrayList<>();
    actualInstrs.addAll(Arrays.asList(selfLoadCode));
    actualInstrs.add(new ArgInstr(funcDef.getLine(), funcDef.getColumn(), OpCode.LOADC, funcObjIndex));
    actualInstrs.add(new NoArgInstr(funcDef.getLine(), funcDef.getColumn(), OpCode.ALLOCF));
    return actualInstrs;
  }
  
  
  private List<Instruction> compileExpr(ASTNode expr, ConstantPool constantPool, ContextManager contextManager, final boolean needsStorage){
    ArrayList<Instruction> instrs = new ArrayList<>();

    if (expr instanceof Unary) {
      Unary unary = (Unary) expr;
      
      instrs.addAll(compileExpr(unary.getTarget(), constantPool, contextManager, needsStorage));
      
      if (unary.getOp() == OperatorKind.MINUS) {
        //Numerical negation operation
        instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.NEG));
      }
      else {
        //Boolean negation operation
        instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.NOT));
      }
    }
    else if (expr instanceof Str) {
      Str string = (Str) expr;
      
      int index = constantPool.addComponent(new StringConstant(string.getValue()));
      instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADC, index));
    }
    else if (expr instanceof Keyword) {
      Keyword keyword = (Keyword) expr;
      if (keyword.getKeyWord() == ReservedWords.MODULE) {
        instrs.add(new LoadCellInstr(keyword.getLine(), keyword.getColumn(), OpCode.LOADMOD, -1));
      }
      else if (keyword.getKeyWord() == ReservedWords.SELF) {
        IdentifierInfo info = contextManager.getVariable(ReservedWords.SELF.actualWord);

        if (info == null) {
          throw new IllegalArgumentException("Unfound identifier: "+ReservedWords.SELF.actualWord+" | "+contextManager);
        }
        
        instrs.add(info.getLoadInstr());
      }
    }
    else if (expr instanceof Parenthesized) {
      instrs.addAll(compileExpr(((Parenthesized) expr).getExpr(), constantPool, contextManager, needsStorage));
    }
    else if (expr instanceof NullValue) {
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADNULL));
    }
    else if (expr instanceof Int) {
      Int integer = (Int) expr;
      
      int index = constantPool.addComponent(new IntegerConstant(integer.getValue()));
      instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADC, index));
    }
    else if (expr instanceof Identifier) {
      Identifier identifier = (Identifier) expr;
      
      IdentifierInfo info = contextManager.getVariable(identifier.getIdentifier());
      if (info == null) {
        throw new IllegalArgumentException("Unfound identifier: "+identifier.getIdentifier()+" | "+contextManager);
      }
      
      if(needsStorage) {
        //the value on the operand stack needs to be stored in this local variable
        instrs.add(info.getStoreInstr());
      }
      else {
        instrs.add(info.getLoadInstr());
      }
    }
    else if (expr instanceof FloatingPoint) {
      FloatingPoint floating = (FloatingPoint) expr;
      
      int index = constantPool.addComponent(new FloatConstant(floating.getValue()));
      instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADC, index));
    }
    else if (expr instanceof ObjectLiteral) {
      ObjectLiteral dictLiteral = (ObjectLiteral) expr;
      
      //Create function level context-manager
      //ContextManager objectContextManager = new ContextManager(contextManager, ContextType.OBJECT);
      
      //First, allocate an empty object
      //instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.ALLOCO));
      
      //Pushes a shallow/referential duplicate of the empty object
      //instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.DUP));
      
      //Assign the "self" attribute on that object
      //final int selfAttrNameIndex = constantPool.addComponent(new StringConstant(ReservedWords.SELF.actualWord));
      //instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.STOREATTR, selfAttrNameIndex));
      
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.MAKEARGV));

      /*
       * New way of instiating object literals:
       * 
       * We essentially treat it as a function call. Key-value pairs are passed on the operand stack
       * using an ArgVector. We then use the the "allocos" instruction to properly setup this object
       * 
       * If the object literal is empty, we just use "alloco" for an empty object
       */
      
      for (Entry<String, ASTNode> entry : dictLiteral.getKeyValPairs().entrySet()) {      
        final int keyNameIndex = constantPool.addComponent(new StringConstant(entry.getKey()));
        
        //Add instruction for entry value
        List<Instruction> valueInstrs = compileExpr(entry.getValue(), constantPool, contextManager, needsStorage);
        instrs.addAll(valueInstrs);
        instrs.add(new ArgInstr(entry.getValue().getLine(), entry.getValue().getColumn(), OpCode.ARG, keyNameIndex));
      }
      
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.ALLOCO));      
    }
    else if (expr instanceof Bool) {
      Bool bool = (Bool) expr;
      
      int index = constantPool.addComponent(new BoolConstant(bool.getValue()));
      instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADC, index));
    }
    else if (expr instanceof AttrAccess) {
      AttrAccess access = (AttrAccess) expr;
      
      //First, add instructions for target
      instrs.addAll(compileExpr(access.getTarget(), constantPool, contextManager, false));
      
      int attrNameIndex = constantPool.addComponent(new StringConstant(access.getAttrName()));
      
      if(needsStorage) {
        //the value on the operand stack needs to be stored in this attribute
        instrs.add(new StoreCellInstr(expr.getLine(), expr.getColumn(), OpCode.STOREATTR, attrNameIndex));
      }
      else {
        instrs.add(new LoadCellInstr(expr.getLine(), expr.getColumn(), OpCode.LOADATTR, attrNameIndex));
      }
    }
    else if (expr instanceof ArrayLiteral) {
      ArrayLiteral arrayLiteral = (ArrayLiteral) expr;
      
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.MAKEARGV));
      
      //Then, add each array entry
      for (ASTNode entry : arrayLiteral.getArrayValues()) {
        List<Instruction> valueInstrs = compileExpr(entry, constantPool, contextManager, needsStorage);
        instrs.addAll(valueInstrs);
        
        instrs.add(new ArgInstr(expr.getLine(), expr.getColumn(), OpCode.ARG, -1));
      }  
      
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.ALLOCA));      
    }
    else if (expr instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) expr;
      
      //First, load target object
      List<Instruction> targetInstrs = compileExpr(arrayAccess.getTarget(), constantPool, contextManager, needsStorage);
      instrs.addAll(targetInstrs);
      
      //Second, load index object
      List<Instruction> indexInstrs = compileExpr(arrayAccess.getIndexValue(), constantPool, contextManager, needsStorage);      
      instrs.addAll(indexInstrs);
      
      if(needsStorage) {
        instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.STOREIN));
      }
      else {
        instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.LOADIN));
      }
    }
    else if (expr instanceof BinaryOpExpr) {
      return compileBinOp((BinaryOpExpr) expr, constantPool, contextManager, needsStorage);
    }
    else if (expr instanceof FunctionCall) {
      FunctionCall functionCall = (FunctionCall) expr;
      
      instrs.add(new NoArgInstr(expr.getLine(), expr.getColumn(), OpCode.MAKEARGV));
            
      for(CallArg callArg : functionCall.getArguments()) {
        List<Instruction> argInstructions = compileExpr(callArg.getArgument(), constantPool, contextManager, needsStorage);
        instrs.addAll(argInstructions);
        
        int keywordIndex = callArg.isKeywordArg() ? 
            constantPool.addComponent(new StringConstant(callArg.getParamName())) : -1;
        
        instrs.add(new ArgInstr(callArg.getLine(), callArg.getColumn(), OpCode.ARG, keywordIndex));
      }
      
      instrs.addAll(compileExpr(functionCall.getTarget(), constantPool, contextManager, needsStorage));
      
      /*TODO: How to load the "self" object for CALL
      ASTNode callableTarget = unwrap(functionCall.getTarget());
      if(callableTarget instanceof AttrAccess){
        AttrAccess callableAttrTarget = (AttrAccess) callableTarget;
        instrs.addAll(compileExpr(callableAttrTarget.getTarget(), constantPool, contextManager, needsStorage));
      }
      else {
        instrs.add(contextManager.getVariable(ReservedWords.SELF.actualWord).getLoadInstr());
      }
      THIS CODE IS IN DEV */
      
      instrs.add(new NoArgInstr(functionCall.getLine(), functionCall.getColumn(), OpCode.CALL));
    }
    else if (expr instanceof FuncDef) {
      return compileFunction((FuncDef) expr, constantPool, contextManager);
    }
    
    return instrs;
  }
  
  private List<Instruction> compileBinOp(BinaryOpExpr binOpExpr, 
      ConstantPool constantPool, 
      ContextManager contextManager, 
      boolean needsStorage){
    
    ArrayList<Instruction> operandInstrs = new ArrayList<>();
    
    if (binOpExpr.getOperator() == OperatorKind.ASSIGN) {
      //Value instructions should be executed first.
      operandInstrs.addAll(compileExpr(binOpExpr.getRight(), constantPool, contextManager, needsStorage));

      //Then compile the assginee's instructions with the needsStorage flag to be true
      operandInstrs.addAll(compileExpr(binOpExpr.getLeft(), constantPool, contextManager, true));
    }
    else if (OperatorKind.operatorMutatesLeft(binOpExpr.getOperator())) {
      //unpack this expression from: mutatee *= value ===> TO BE: mutatee = mutatee * value
      BinaryOpExpr valueExpr = new BinaryOpExpr(binOpExpr.getLine(), 
                                                 binOpExpr.getColumn(), 
                                                 OperatorKind.getMutatorOperator(binOpExpr.getOperator()), 
                                                 binOpExpr.getLeft(), 
                                                 binOpExpr.getRight());
      
      BinaryOpExpr asigneeExpr = new BinaryOpExpr(binOpExpr.getLine(), 
          binOpExpr.getColumn(), 
          OperatorKind.ASSIGN, 
          binOpExpr.getLeft(), 
          valueExpr);
      
      //System.out.println("--- asignee expr: "+asigneeExpr);
      
      return compileBinOp(asigneeExpr, constantPool, contextManager, false);
    }
    else if(binOpExpr.getOperator() == OperatorKind.BOOL_AND) {
      final String operandFalse = genLabelName("sc_op_false");
      final String endBranch =  genLabelName("sc_done");
      
      operandInstrs.addAll(compileExpr(binOpExpr.getLeft(), constantPool, contextManager, needsStorage));
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMPF, operandFalse));
      operandInstrs.addAll(compileExpr(binOpExpr.getRight(), constantPool, contextManager, needsStorage));
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMPF, operandFalse));
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMP, endBranch));

      operandInstrs.add(new LabelInstr(binOpExpr.getLine(), binOpExpr.getColumn(), operandFalse));
      final int falseConstantAddr = constantPool.addComponent(new BoolConstant(false));
      operandInstrs.add(new ArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LOADC, falseConstantAddr));
      operandInstrs.add(new LabelInstr(binOpExpr.getLine(), binOpExpr.getColumn(), endBranch));
    }
    else if(binOpExpr.getOperator() == OperatorKind.BOOL_OR) {
      final String operandTrue = genLabelName("sc_op_true");
      final String endBranch =  genLabelName("sc_done");

      operandInstrs.addAll(compileExpr(binOpExpr.getLeft(), constantPool, contextManager, needsStorage));
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMPT, operandTrue));
      operandInstrs.addAll(compileExpr(binOpExpr.getRight(), constantPool, contextManager, needsStorage));
      
      
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMPT, operandTrue));
      final int falseConstantAddr = constantPool.addComponent(new BoolConstant(false));
      operandInstrs.add(new ArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LOADC, falseConstantAddr));
      
      operandInstrs.add(new JumpInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.JUMP, endBranch));

      operandInstrs.add(new LabelInstr(binOpExpr.getLine(), binOpExpr.getColumn(), operandTrue));
      final int trueConstantAddr = constantPool.addComponent(new BoolConstant(true));
      operandInstrs.add(new ArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LOADC, trueConstantAddr));
      
      
      operandInstrs.add(new LabelInstr(binOpExpr.getLine(), binOpExpr.getColumn(), endBranch));
    }
    else if (binOpExpr.getOperator() == OperatorKind.ARROW) {
      operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.MAKEARGV));
      
      operandInstrs.addAll(compileExpr(binOpExpr.getLeft(), constantPool, contextManager, needsStorage));
      operandInstrs.add(new ArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.ARG, -1));
      
      operandInstrs.addAll(compileExpr(binOpExpr.getRight(), constantPool, contextManager, needsStorage));
      operandInstrs.add(new ArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.ARG, -1));
      
      final int systemModuleName = constantPool.addComponent(new StringConstant("system"));
      final int bindName = constantPool.addComponent(new StringConstant("bind"));
      
      operandInstrs.add(new LoadCellInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LOADMV, systemModuleName));
      operandInstrs.add(new LoadCellInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LOADATTR, bindName));
      operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.CALL));
    }
    else {
      operandInstrs.addAll(compileExpr(binOpExpr.getLeft(), constantPool, contextManager, needsStorage));
      operandInstrs.addAll(compileExpr(binOpExpr.getRight(), constantPool, contextManager, needsStorage));
      
      switch (binOpExpr.getOperator()) {
      case PLUS: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.ADD));
        break;
      }
      case MINUS: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.SUB));
        break;
      }
      case TIMES: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.MUL));
        break;
      }
      case DIV: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.DIV));
        break;
      }
      case MOD: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.MOD));
        break;
      }
      case LESS: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LESS));
        break;
      }
      case GREAT: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.GREAT));
        break;
      }
      case GREATQ: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.GREATE));
        break;
      }
      case LESSQ: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.LESSE));
        break;
      }
      case IS: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.REQUAL));
        break;
      }
      case EQUAL: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.EQUAL));
        break;
      }
      case NOTEQUAL: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.NOTEQUAL));
        break;
      }
      case BIT_AND: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.BAND));
        break;
      }
      case BIT_OR: {
        operandInstrs.add(new NoArgInstr(binOpExpr.getLine(), binOpExpr.getColumn(), OpCode.BOR));
        break;
      }
      default:
        throw new IllegalArgumentException("Unknown binary operator: "+binOpExpr.getOperator());
      }
    }

    
    return operandInstrs;
  }
  
  private static ASTNode unwrap(ASTNode expr) {
    while (expr instanceof Parenthesized) {
      expr = ((Parenthesized) expr).getExpr();
    }
    return expr;
  }
  
  
  private static long labelTag = 0;
  
  private static String genLabelName(String labelName) {
    String ret = labelName+labelTag;
    labelTag++;
    return ret;
  }
}
