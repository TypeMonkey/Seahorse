package jg.sh.compile.validation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jg.sh.common.OperatorKind;
import jg.sh.common.presenters.VariablePresenter;
import jg.sh.compile.CompilationException;
import jg.sh.compile.parsing.nodes.BinaryOpExpr;
import jg.sh.compile.parsing.nodes.CallArg;
import jg.sh.compile.parsing.exceptions.RepeatedComponentNameException;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.FunctionCall;
import jg.sh.compile.parsing.nodes.ReservedWords;
import jg.sh.compile.parsing.nodes.atoms.ArrayAccess;
import jg.sh.compile.parsing.nodes.atoms.ArrayLiteral;
import jg.sh.compile.parsing.nodes.atoms.AttrAccess;
import jg.sh.compile.parsing.nodes.atoms.ObjectLiteral;
import jg.sh.compile.parsing.nodes.atoms.FuncDef;
import jg.sh.compile.parsing.nodes.atoms.Identifier;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.NullValue;
import jg.sh.compile.parsing.nodes.atoms.constants.Constant;
import jg.sh.compile.parsing.nodes.atoms.Parenthesized;
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
import jg.sh.compile.validation.Context.ContextType;
import jg.sh.compile.validation.SymbolTable.SymbolContext;
import jg.sh.compile.validation.exceptions.BadKeywordPlacement;
import jg.sh.compile.validation.exceptions.InvalidReassignmentException;
import jg.sh.compile.validation.exceptions.UnfoundComponentException;

/**
 * Validates the structure of a source file, such as checking 
 * for invalid sequences of statements (dead code, bad block sequences, etc.)
 * and missing modules, inheritance, functions and first-level identifiers
 * @author Jose
 *
 */
public class Validator {
  
  private static final VariablePresenter SELF = new VariablePresenter(ReservedWords.SELF.actualWord.toLowerCase(), ReservedWords.CONST);
  private static final VariablePresenter MODULE = new VariablePresenter(ReservedWords.MODULE.actualWord.toLowerCase(), ReservedWords.CONST);
  
  //private final SymbolTable moduleTable;
  
  /**
   * Constructs a Validator
   */
  public Validator() {
    //this.moduleTable = new SymbolTable(new Context());
  }
  
  /**
   * Validates the FileConstructs 
   * @param sourceFiles - the source files to validate
   * @return a map of the source file's name and validation errors encountered with them
   */
  public Map<String, FileValidationReport> validate(Module ... sourceFiles) {  
    /*
    SymbolTable systemTable = new SymbolTable(null, null, null, null);
    //add all accessible modules to the systemTable
    availableModules.values().forEach(x -> systemTable.addModule(x));
    
    //only add functions and classes from the System module!
    systemModule.getFunctions().values().forEach(x -> systemTable.addFunction(x));
    systemModule.getTypeDefs().values().forEach(x -> systemTable.addClass(x));
    
    //Generate ModulePresenters for all source modules
    for(Module source: sourceFiles) {
      systemTable.addModule(source.getPresenter());
    }
    */
    
    //now, validate all files.
    HashMap<String, FileValidationReport> checkedFiles = new HashMap<>();
    for(Module source: sourceFiles) {      
      FileValidationReport report = validateFile(source);
      checkedFiles.put(source.getName(), report);
    }
    
    return checkedFiles;   
  }
  
  /**
   * Validates a FileConstruct
   * @param construct - the FileConstruct to validate
   * @param table - the parent SymbolTable to use
   * @return the FileValidationReport as a result of validating the given FileConstruct
   */
  private FileValidationReport validateFile(Module construct) {
    //System.out.println("---- validating module: "+construct.getName());
    ArrayList<CompilationException> vErrors = new ArrayList<>();
    
    //Creates a module-level context
    final Context context = new Context();
    
    //symbol table from all imported components
    SymbolTable moduleTable = new SymbolTable(context);
    
    //Add imported modules and components to the module table
    for(UseStatement imprt : construct.getImports()) {
      if (imprt.getComponents().size() == 0) {
        //use expressions with no component imports are just module imports
        moduleTable.addVariable(new VariablePresenter(imprt.getTargetModule()));
      }
      else {
        // add components as their respective type
        for(String compName : imprt.getComponents()) {
          moduleTable.addVariable(new VariablePresenter(compName));
        }
      }
    }
    
    //add the module keyword to the module table
    moduleTable.addVariable(MODULE);
        
    /*
    //add all data definitions and functions to module table
    construct.getStatements().forEach(statement -> {
      if (statement instanceof DataDefinition) {
        DataDefinition dataDefinition = (DataDefinition) statement;
        moduleTable.addVariable(new VariablePresenter(dataDefinition.getName(), 
                                                      Keyword.toReservedWords(dataDefinition.getModifiers())));
      }
      else if (statement instanceof FuncDef) {
        FuncDef funcDef = (FuncDef) statement;
        moduleTable.addVariable(new VariablePresenter(funcDef.getName(), 
                                                      Keyword.toReservedWords(funcDef.getModifiers())));
      }
    });
    */
    
    //System.out.println("----validating statements of module "+construct.getName()+" | "+construct.getStatements());
    //now, go through all module statements and validate them
    for(Statement statement : construct.getStatements()) {
      //System.out.println("    ==> "+statement);
      if (!(statement instanceof VariableStatement)) {
        vErrors.addAll(validateStmt(statement, construct.getName(), moduleTable));
      }
      else {
        VariableStatement varStatement = (VariableStatement) statement;
        vErrors.addAll(validateVar(varStatement, construct.getName(), moduleTable));
        moduleTable.addVariable(varStatement.getPresenter());
      }
    }
    
    return new FileValidationReport(construct, vErrors);
  }
  
  /*
  private List<CompilationException> validateTypeDef(DataDefinition construct, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();
    
    final Context classContext = new Context(ContextType.CLASS, table.getTableContext());

    //Create a symbol table for the class
    SymbolTable classTable = new SymbolTable(table, classContext);
    classTable.addVariable(SELF);
    
    //add a class' methods in the symbol table as a symbol
    construct.getMethods().values().forEach(method -> {
      classTable.addVariable(new VariablePresenter(method.getName(), Keyword.toReservedWords(method.getModifiers())));
    });
    
    construct.getMethods().values().forEach(
        x -> vErrors.addAll(validateFunc(x, hostModuleName, classTable, x.getName().equals(ReservedWords.CONSTR.actualWord)))
    );
    
    return vErrors;
  }
  */
  
  /*
  private List<CompilationException> validateFunc(FuncDef funcDef, String hostModuleName, SymbolTable table, boolean isConstructor){
    ArrayList<CompilationException> vErrors = new ArrayList<>();
    
    final Context funcContext = isConstructor ? 
                                   new Context(ContextType.CONSTR, table.getTableContext()) : 
                                   new Context(ContextType.FUNCTION, table.getTableContext());
    
    //Create a symbol table for the function, with all its parameters
    SymbolTable funcTable = new SymbolTable(table, funcContext);
    
    //iterate through parameters
    for (Parameter parameter : funcDef.getParams().values()) {
      System.out.println("*** IN FUNC DEF: "+parameter);
      if (parameter.isConst()) {
        funcTable.addVariable(new VariablePresenter(parameter.getName(), ReservedWords.CONST));
      }
      else {
        funcTable.addVariable(new VariablePresenter(parameter.getName()));
      }
    }
    
    //go through the function's statements
    vErrors.addAll(validateStmtList(funcDef.getStatements(), hostModuleName, funcTable));

    return vErrors;
  }
  */
  
  private List<CompilationException> validateVar(VariableStatement var, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();

    final Set<ReservedWords> modifiers = Keyword.toReservedWords(var.getModifiers());
    
    if(modifiers.contains(ReservedWords.EXPORT) && table.getTableContext().isWithinContext(ContextType.FUNCTION)) {
      vErrors.add(new BadKeywordPlacement(ReservedWords.EXPORT, hostModuleName, var.getLine(), var.getColumn(), ContextType.MODULE));
    }
    
    vErrors.addAll(validateExpr(var.getAssgnedExpr(), hostModuleName, table));
    return vErrors;
  }
  
  private List<CompilationException> validateStmtList(List<Statement> statements, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();
    
    //Found VariableStatements will be placed in the provided SymbolTable
    
    //Used to check if two symbols occupy the same immediate scope
    HashSet<String> immediateScope = new HashSet<>();
    
    for (Statement statement : statements) {
      
      //System.out.println("----> VALIDATE STMT: "+statement);
      
      if (statement instanceof VariableStatement) {       
        VariableStatement var = (VariableStatement) statement;
        
        if (immediateScope.contains(var.getName())) {
          vErrors.add(new RepeatedComponentNameException(var.getName(), hostModuleName, var.getLine(), var.getColumn()));
        }
        
        vErrors.addAll(validateVar(var, hostModuleName, table));
        table.addVariable(var.getPresenter());      
      }
      else {
        vErrors.addAll(validateStmt(statement, hostModuleName, table));
      }
    }
    
    return vErrors;
  }
  
  private List<CompilationException> validateBlock(Block block, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();
    
    final Context blockContext = new Context(ContextType.BLOCK, table.getTableContext());
    
    if (block instanceof IfElse) {
      IfElse ifElse = (IfElse) block;
      
      //check initial condition
      vErrors.addAll(validateExpr(ifElse.getCond(), hostModuleName, table));
      
      //check the initial condition's block
      vErrors.addAll(validateStmtList(ifElse.getBranchCode(), hostModuleName, new SymbolTable(table, blockContext)));
      
      //check other branches
      for (IfElse branch : ifElse.getOtherBranches()) {
        vErrors.addAll(validateBlock(branch, hostModuleName, new SymbolTable(table, blockContext)));
      }
    }
    else if (block instanceof WhileLoop) {
      WhileLoop loop = (WhileLoop) block;
      
      //check the loop condition
      vErrors.addAll(validateExpr(loop.getCondition(), hostModuleName, table));
            
      final Context loopContext = new Context(ContextType.LOOP, table.getTableContext());
      final SymbolTable loopTable = new SymbolTable(table, loopContext);
      
      //then check the loop's body
      vErrors.addAll(validateStmtList(loop.getStatements(), hostModuleName, loopTable));
      
    }
    else if (block instanceof TryCatch) {
      TryCatch tryCatch = (TryCatch) block;
      
      //check the try-body first
      vErrors.addAll(validateStmtList(tryCatch.getTargetBlock(), hostModuleName, new SymbolTable(table, blockContext)));
      
      //create a new SymbolTable for the handling block with the exception variable
      SymbolTable handleTable = new SymbolTable(table, blockContext);
      handleTable.addVariable(new VariablePresenter(tryCatch.getErrorVar().getIdentifier()));
      
      //now check the handle body
      vErrors.addAll(validateStmtList(tryCatch.getHandleBlock(), hostModuleName, handleTable));   
    }
    else if (block instanceof ScopeBlock) {
      ScopeBlock scope = (ScopeBlock) block;
      
      //check the list of the scope block
      vErrors.addAll(validateStmtList(scope.getStatements(), hostModuleName, new SymbolTable(table, blockContext)));
    }
    
    return vErrors;
  }
  
  private List<CompilationException> validateStmt(Statement stmt, String hostModuleName, SymbolTable table){
    final ArrayList<CompilationException> statementErrors = new ArrayList<>();
        
    /*
    System.out.println("-----------STMT VALIDATION: "+statementErrors+" | "+stmt);
    System.out.println("          => "+stmt.getModifiers());
    System.out.println("          => "+table.getTableContext());
    System.out.println("          => "+table.getTableContext().isWithinContext(ContextType.LOOP));
    */
    
    if (stmt instanceof Block) {
      Block block = (Block) stmt;
      statementErrors.addAll(validateBlock(block, hostModuleName, table));
    }
    else if (stmt instanceof ExpressionStatement) {
      ExpressionStatement statement = (ExpressionStatement) stmt;
      
      if(statement.hasModifier(ReservedWords.RETURN) && !table.getTableContext().isWithinContext(ContextType.FUNCTION)) {
        statementErrors.add(new BadKeywordPlacement(ReservedWords.RETURN, 
            ContextType.FUNCTION, 
            hostModuleName, 
            stmt.getLine(), 
            stmt.getColumn()));
      }
      
      statementErrors.addAll(validateExpr(statement.getExpr(), hostModuleName, table));
    }
    else if (stmt instanceof KeywordStatement) {
      KeywordStatement keywordStatement = (KeywordStatement) stmt;
      
      final Context loopContext = table.getTableContext().getClosestContext(ContextType.LOOP);
      final Context functionContext = table.getTableContext().getClosestContext(ContextType.FUNCTION);
      
      if (keywordStatement.getReservedWord() == ReservedWords.BREAK || 
          keywordStatement.getReservedWord() == ReservedWords.CONTINUE) {
        if(loopContext == null || 
           (functionContext != null && functionContext.getClosestContext(ContextType.LOOP) == loopContext)) {
          //break and continue are only allowed within loops, unless the break and continue are
          //within anon functions. In that case, it's not allowed
          
          /*
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
          statementErrors.add(new BadKeywordPlacement(keywordStatement.getReservedWord(), 
              ContextType.LOOP, 
              hostModuleName, 
              stmt.getLine(), 
              stmt.getColumn()));
        }
      }
      else if (keywordStatement.getReservedWord() == ReservedWords.RETURN) {
        if (!table.getTableContext().isWithinContext(ContextType.FUNCTION)) {
          statementErrors.add(new BadKeywordPlacement(ReservedWords.RETURN, 
              ContextType.FUNCTION, 
              hostModuleName, 
              stmt.getLine(), 
              stmt.getColumn()));
        }
      }
      
    }

    return statementErrors;
  } 
  
  private List<CompilationException> validateFunc(FuncDef func, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();

    //Make function context
    final Context anonFuncContext = new Context(ContextType.FUNCTION, table.getTableContext());
    
    //Make function table
    final SymbolTable funcTable = new SymbolTable(table.getTable(ContextType.MODULE), anonFuncContext);
    
    //add "self" to function table
    funcTable.addVariable(SELF);
    
    //If this function has a bound name, add it to it's symbol to allow for recursion
    if (func.hasName()) {
      //Make sure the function's name is constant to disallow for reassignment
      funcTable.addVariable(new VariablePresenter(func.getBoundName(), ReservedWords.CONST));
    }
    
    //add function parameters
    func.getParams().values().forEach(param -> {
      funcTable.addVariable(new VariablePresenter(param.getName(), 
                                                  param.isConst() ? new HashSet<>(Arrays.asList(ReservedWords.CONST)) : 
                                                                    new HashSet<>()));
    });
    
    //Look over all capture statements. See if those captures variables actually exists
    func.getCaptures().forEach(var -> {
      SymbolContext freeVarContext = table.getSymbol(var.getName());
      
      //System.out.println("----FOUND CAPTURE? "+var.getName()+"|"+(freeVarContext != null)+" | "+table.getAccessibleSymbols());
      
      if (freeVarContext == null) {
        vErrors.add(new UnfoundComponentException(var.getName(), 
            hostModuleName, 
            var.getLine(), 
            var.getColumn()));
      }
      else if (freeVarContext.getPresenter().getKeywords().contains(ReservedWords.CONST) && 
               !var.getPresenter().getKeywords().contains(ReservedWords.CONST)) {
        /*
         * If a constant variable has been captured and set to be mutable, then this is illegal
         */
        vErrors.add(new InvalidReassignmentException(var.getName(), hostModuleName, var.getLine(), var.getColumn()));
      }
      else if (funcTable.isSymbolAccessible(var.getName())) {
        vErrors.add(new RepeatedComponentNameException(var.getName(), hostModuleName, var.getLine(), var.getColumn()));
      }
      else {
        //System.out.println("        >>>CAPTURE: "+freeVarContext.getPresenter());
        funcTable.addVariable(var.getPresenter());
      }
    });
    
    //now, validate all statements
    vErrors.addAll(validateStmtList(func.getStatements(), hostModuleName, funcTable));
    
    return vErrors;
  }
  
  private List<CompilationException> validateExpr(ASTNode expr, String hostModuleName, SymbolTable table){
    ArrayList<CompilationException> vErrors = new ArrayList<>();

    if (expr instanceof Constant ||
        expr instanceof NullValue) {
      //do nothing
    }
    else if (expr instanceof Keyword) {
      Keyword keyword = (Keyword) expr;
      
      //System.out.println("self or module! "+keyword.getKeyWord()+"  || "+table.getSymbol(keyword.getKeyWord().actualWord.toLowerCase()));
      
      if (!table.isSymbolAccessible(keyword.getKeyWord().actualWord.toLowerCase())) {
        vErrors.add(new UnfoundComponentException(keyword.getKeyWord().actualWord.toLowerCase(), 
                                                  hostModuleName, 
                                                  keyword.getLine(), 
                                                  keyword.getColumn()));
      }
    }
    else if (expr instanceof FuncDef) {
      FuncDef anonFunc = (FuncDef) expr;      
      vErrors.addAll(validateFunc(anonFunc, hostModuleName, table));
    }
    else if (expr instanceof ArrayAccess) {
      ArrayAccess arrayAccess = (ArrayAccess) expr;
      
      //validate the target
      vErrors.addAll(validateExpr(arrayAccess.getTarget(), hostModuleName, table));
      
      //validate the index expr
      vErrors.addAll(validateExpr(arrayAccess.getIndexValue(), hostModuleName, table));
    }
    else if (expr instanceof ArrayLiteral) {
      ArrayLiteral arrayLiteral = (ArrayLiteral) expr;
      
      //validate each array entry
      for (ASTNode entry : arrayLiteral.getArrayValues()) {
        vErrors.addAll(validateExpr(entry, hostModuleName, table));
      }
    }
    else if (expr instanceof AttrAccess) {
      AttrAccess attrAccess = (AttrAccess) expr;
      
      //validate the target of the access
      vErrors.addAll(validateExpr(attrAccess.getTarget(), hostModuleName, table));
    }
    else if (expr instanceof ObjectLiteral) {
      ObjectLiteral dictLiteral = (ObjectLiteral) expr;
      
      //Make a new table for Object literal and add the "self" identifier
      SymbolTable objectTable = new SymbolTable(table, new Context(ContextType.OBJECT, table.getTableContext()));
      objectTable.addVariable(SELF);
      
      //validate each dictionary value
      dictLiteral.getKeyValPairs().values().forEach(x -> vErrors.addAll(validateExpr(x, hostModuleName, objectTable)));
    }
    else if (expr instanceof Identifier) {
      Identifier identifier = (Identifier) expr;
      
      //System.out.println("symbol! "+identifier.getIdentifier()+"  || "+table.getSymbol(identifier.getIdentifier()));
      
      if (!table.isSymbolAccessible(identifier.getIdentifier())) {
        vErrors.add(new UnfoundComponentException(identifier.getIdentifier(), 
                                                  hostModuleName, 
                                                  identifier.getLine(), 
                                                  identifier.getColumn()));
      }
    }
    else if (expr instanceof Parenthesized) {  
      vErrors.addAll(validateExpr(unwrap(expr), hostModuleName, table));
    }
    else if (expr instanceof BinaryOpExpr) {
      BinaryOpExpr binaryOpExpr = (BinaryOpExpr) expr;
      
      //check if this is a reassignment
      ASTNode leftOperandUnwrapped = unwrap(binaryOpExpr.getLeft());
      if (leftOperandUnwrapped instanceof Identifier && 
          OperatorKind.operatorMutatesLeft(binaryOpExpr.getOperator())) {
        Identifier leftIdentifier = (Identifier) leftOperandUnwrapped;
        
        SymbolContext targetContext = table.getSymbol(leftIdentifier.getIdentifier());
        if(targetContext != null) {
          VariablePresenter targetVar = targetContext.getPresenter();
          if (targetVar.getKeywords().contains(ReservedWords.CONST)) {
            vErrors.add(new InvalidReassignmentException(leftIdentifier.getIdentifier(), 
                                                         hostModuleName, 
                                                         leftIdentifier.getLine(), 
                                                         leftIdentifier.getColumn()));
          }
        }
        else {
          vErrors.add(new UnfoundComponentException(leftIdentifier.getIdentifier(), 
                                                    hostModuleName, 
                                                    leftIdentifier.getLine(), 
                                                    leftIdentifier.getColumn()));  
        }
      }
      
      //validate the left operand
      vErrors.addAll(validateExpr(binaryOpExpr.getLeft(), hostModuleName, table));
      
      //validate the right operand
      vErrors.addAll(validateExpr(binaryOpExpr.getRight(), hostModuleName, table));
    }
    else if (expr instanceof FunctionCall) {
      FunctionCall functionCall = (FunctionCall) expr;
      
      //System.out.println("FUNC CALL: "+functionCall);
      
      //validate the target of the call
      vErrors.addAll(validateExpr(functionCall.getTarget(), hostModuleName, table));
      
      //counts the amount of non-optional arguments given to the function
      //int nonOptionalArgs = 0;
      
      //validate each argument
      for(CallArg arg : functionCall.getArguments()) {
        vErrors.addAll(validateExpr(arg.getArgument(), hostModuleName, table));
      }
      
      /*
       * This check is redundant and assumes that the called variable is constant
      if (functionCall.getTarget() instanceof Identifier) {
        Identifier funcName = (Identifier) functionCall.getTarget();
        FunctionSignature targetFunc = table.findFunction(funcName.getIdentifier());
        if (targetFunc != null) {
          
          //counts the amount of non-optional arguments expected by the function
          int requiredParams = 0;
          
          for (Entry<String, Boolean> paramEntry: targetFunc.getParameters().entrySet()) {
            if (!paramEntry.getValue()) {
              requiredParams++;
            }
          }
          
          //if not equal, this is a problem.
          if (nonOptionalArgs != requiredParams) {
            vErrors.add(new CompilationException(funcName.getIdentifier()+" expects "+requiredParams+" non-optional arguments, but only "+nonOptionalArgs+" were given", 
                                                  hostModuleName, 
                                                  functionCall.getLine(), 
                                                  functionCall.getColumn()));
          }
          
          //check if the optional arguments given by the callee is an optional parameter in the function
          for (CallArg arg : functionCall.getArguments()) {
            if (arg.isOptionalArg() && !targetFunc.getParameters().containsKey(arg.getParamName())) {
              vErrors.add(new CompilationException(funcName.getIdentifier()+" has no optional argument '"+arg.getParamName()+"'", 
                  hostModuleName, 
                  functionCall.getLine(), 
                  functionCall.getColumn()));
            }
          }
        }
      }
      */
    }
    
    return vErrors;
  }
  
  private ASTNode unwrap(ASTNode expr) {
    while (expr instanceof Parenthesized) {
      expr = ((Parenthesized) expr).getExpr();
    }
    return expr;
  }
  
  
  /*
  private List<Map<String, VariablePresenter>> wrap(Map<String, VariablePresenter> map){
    ArrayList<Map<String, VariablePresenter>> newList = new ArrayList<>();
    newList.add(map);
    return newList;
  }
  
  private VariablePresenter find(List<Map<String, VariablePresenter>> maps, String name) {
    for(int i = maps.size() - 1; i >= 0; i--) {
      if (maps.get(i).containsKey(name)) {
        return maps.get(i).get(name);
      }
    }
    return null;
  }
  */
}
