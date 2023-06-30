package jg.sh.compile_old.parsing.parser;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Stack;

import jg.sh.common.FunctionSignature;
import jg.sh.common.OperatorKind;
import jg.sh.compile_old.parsing.exceptions.FormationException;
import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.BinaryOpExpr;
import jg.sh.compile_old.parsing.nodes.CallArg;
import jg.sh.compile_old.parsing.nodes.FunctionCall;
import jg.sh.compile_old.parsing.nodes.ReservedWords;
import jg.sh.compile_old.parsing.nodes.atoms.ArrayAccess;
import jg.sh.compile_old.parsing.nodes.atoms.ArrayLiteral;
import jg.sh.compile_old.parsing.nodes.atoms.AttrAccess;
import jg.sh.compile_old.parsing.nodes.atoms.FuncDef;
import jg.sh.compile_old.parsing.nodes.atoms.Identifier;
import jg.sh.compile_old.parsing.nodes.atoms.Keyword;
import jg.sh.compile_old.parsing.nodes.atoms.NullValue;
import jg.sh.compile_old.parsing.nodes.atoms.ObjectLiteral;
import jg.sh.compile_old.parsing.nodes.atoms.Operator;
import jg.sh.compile_old.parsing.nodes.atoms.Parameter;
import jg.sh.compile_old.parsing.nodes.atoms.Parenthesized;
import jg.sh.compile_old.parsing.nodes.atoms.Unary;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Bool;
import jg.sh.compile_old.parsing.nodes.atoms.constants.FloatingPoint;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Int;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Str;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.IfElse;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.ScopeBlock;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.TryCatch;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.WhileLoop;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.CaptureStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.ExpressionStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.KeywordStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.UseStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.VariableStatement;
import net.percederberg.grammatica.parser.Node;
import net.percederberg.grammatica.parser.ParseException;
import net.percederberg.grammatica.parser.Production;
import net.percederberg.grammatica.parser.Token;


public class ExpressionBuilder extends SeaHorseWholeAnalyzer {
  
  final static String ANON_FUNC_NAME = "$anon_func";
  
  /**
   * Names that cannot be used in a Seahorse program to identify functions, variables, classes or files/modules
   */
  public static final Set<String> INVALID_IDENTIFIERS;
  
  static {
    HashSet<String> badIdentifiers = new HashSet<>(); 
    
    for (ReservedWords badIden : ReservedWords.values()) {
      badIdentifiers.add(badIden.actualWord);
    }
    
    INVALID_IDENTIFIERS = Collections.unmodifiableSet(badIdentifiers);
  }
  
  protected final Stack<ArrayDeque<ASTNode>> stack;
  protected final Stack<ASTNode> actualNodes;
  
  private String fileName;
 
  private Module rawModule;
  
  public ExpressionBuilder(String fileName) {
    this.fileName = fileName;
    this.stack = new Stack<>();
    this.actualNodes = new Stack<>();
  }
  
  public void setFileName(String fileName) {
    this.fileName = fileName;
  }
  
  public String getFileName() {
    return fileName;
  }
  
  //START of value literal tokens
  @Override
  protected Node exitString(Token node) throws ParseException {
    String actualString = node.getImage();
    actualString = actualString.substring(1);
    actualString = actualString.substring(0, actualString.length() - 1);
    actualNodes.push(new Str(node.getStartLine(), node.getStartColumn(), actualString));
    return node;
  }
  
  @Override
  protected Node exitInteger(Token node) throws ParseException {
    actualNodes.push(new Int(node.getStartLine(), node.getStartColumn(), Long.parseLong(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitDouble(Token node) throws ParseException {
    actualNodes.push(new FloatingPoint(node.getStartLine(), node.getStartColumn(), Double.parseDouble(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitTrue(Token node) throws ParseException {
    actualNodes.push(new Bool(node.getStartLine(), node.getStartColumn(), Boolean.parseBoolean(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitFalse(Token node) throws ParseException {
    actualNodes.push(new Bool(node.getStartLine(), node.getStartColumn(), Boolean.parseBoolean(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitNull(Token node) throws ParseException {
    actualNodes.push(new NullValue(node.getStartLine(), node.getStartColumn()));
    return node;
  }
  
  @Override
  protected Node exitName(Token node) throws ParseException {
    actualNodes.push(new Identifier(node.getStartLine(), node.getStartColumn(), node.getImage()));
    return node;
  }
  //END of value literal tokens
  
  //START of binary operators
  @Override
  protected Node exitIs(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitArrow(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitPlus(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitMinus(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitMult(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitDiv(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitMod(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitLess(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitGreat(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqual(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqEq(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitNotEq(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitGrEq(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitLsEq(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqMult(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqAdd(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqMin(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqDiv(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitEqMod(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitAnd(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitOr(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
    
  @Override
  protected Node exitBoolAnd(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitBoolOr(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitBang(Token node) throws ParseException {
    actualNodes.push(new Operator(node.getStartLine(), node.getStartColumn(), Operator.stringToOp(node.getImage())));
    return node;
  }
  //END of binary operators
  
  //START of keywords
  @Override
  protected Node exitConst(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
    
  @Override
  protected Node exitVar(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitConstr(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitFunc(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitBreak(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitCont(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitReturn(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitUse(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitFrom(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitExport(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitIf(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitElif(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitCatch(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitElse(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitWhile(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitTry(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  /*
  @Override
  protected Node exitNew(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  */
    
  @Override
  protected Node exitData(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitOpSqBrack(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitOpCuBrack(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitModule(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitSelf(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitCapture(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  
  @Override
  protected Node exitThrow(Token node) throws ParseException {
    actualNodes.push(new Keyword(node.getStartLine(), node.getStartColumn(), Keyword.getEquivalentReservedWord(node.getImage())));
    return node;
  }
  //END of keywords
  
  @Override
  protected void enterSourceFile(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitSourceFile(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println("****> SOURCE FILE: "+exprs);
    
    //List of statements for FileConstruct
    ArrayList<Statement> statements = new ArrayList<>();
    
    //List of imports for FileConstructs
    ArrayList<UseStatement> imports = new ArrayList<>();
    
    //add system module imports 
    //final String [] systemComps = Arrays.stream(SystemPresenter.getSystemSymbols())
    //                                    .map(x -> x.getName()).toArray(String[]::new);
    
    boolean importedSystem = false;
        
    while (!exprs.isEmpty()) {
      ASTNode expr = exprs.poll();
      if (expr instanceof UseStatement) {
        UseStatement useStatement = (UseStatement) expr;
        imports.add(useStatement);
        
        importedSystem = !importedSystem ? 
            useStatement.getTargetModule().equals("system") && useStatement.getComponents().isEmpty() : 
              true;
      }
      else {
        statements.add((Statement) expr);
      }
    }
    
    if (!importedSystem) {
      //add system module import if not already imported
      imports.add(new UseStatement(0, 0, "system"));
    }
        
    rawModule = new Module(fileName, imports, statements);
    
    //System.out.println("AFTER MODULE: "+rawModule.getStatements());
    
    return node;
  }
  
  
  @Override
  protected void enterUseStatement(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitUseStatement(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    //System.out.println("USE STATEMENT: "+exprs);

    Keyword prefix = (Keyword) exprs.pollFirst();
        
    if (prefix.getKeyWord() == ReservedWords.USE) {
      Identifier targetModule = (Identifier) exprs.pollFirst();
      actualNodes.push(new UseStatement(prefix.getLine(), prefix.getColumn(), targetModule.getIdentifier()));
    }
    else {
      Identifier targetModule = (Identifier) exprs.pollFirst();
      
      //remove the "use" keyword
      exprs.pollFirst();
      
      String [] compNames = new String[exprs.size()];
      for (int i = 0; i < compNames.length; i++) {
        compNames[i] = ((Identifier) exprs.pollFirst()).getIdentifier();
      }
      
      actualNodes.push(new UseStatement(prefix.getLine(), prefix.getColumn(), targetModule.getIdentifier(), compNames));
    }
    
    return node;
  }
  
  
  @Override
  protected void enterVariable(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitVariable(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println(" =====> VARIABLE: "+exprs);
    
    HashSet<Keyword> modifiers = new HashSet<>();
    
    while (!(exprs.peekFirst() instanceof Identifier)) {
      Keyword currKeyword = (Keyword) exprs.pollFirst();
      modifiers.add(currKeyword);
    }
    
    Identifier name = (Identifier) exprs.pollFirst();
    
    //discard equal sign
    exprs.pollFirst();
    
    ASTNode value = exprs.pollFirst();
    
    //System.out.println("--      => VAR DEC: "+modifiers);
    
    actualNodes.push(new VariableStatement(name.getLine(), 
                                           name.getColumn(), 
                                           name.getIdentifier(), 
                                           value, 
                                           modifiers.toArray(new Keyword[modifiers.size()])));
    
    return node;
  }
  
  @Override
  protected void enterCaptureStatement(Production node) throws ParseException {
    setEntrance();
    //System.out.println("-------->ENTER CAPTURE STATEMENT: "+actualNodes);
  }
  
  @Override
  protected Node exitCaptureStatement(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();

    //System.out.println("----> EXIT CAPTURE STATEMENT: "+exprs);
    
    //Pop 'capture' keyword
    Keyword captureKeyword = (Keyword) exprs.pollFirst();
    
    //Now, pop the names with this capture statement
    HashSet<VariableStatement> capturedVars = new HashSet<>();
    
    while (!exprs.isEmpty()) {
      Keyword [] declaringKeywords = new Keyword[0];
      if(exprs.peekFirst() instanceof Keyword) {
        Keyword keyword = (Keyword) exprs.pollFirst();
        if (keyword.getKeyWord() == ReservedWords.CONST) {
          declaringKeywords = new Keyword[1];
          declaringKeywords[0] = keyword;
        }
      }
      
      Identifier varIdentifier = (Identifier) exprs.pollFirst();
      VariableStatement varStatement = new VariableStatement(varIdentifier.getLine(), 
                                                             varIdentifier.getColumn(), 
                                                             varIdentifier.getIdentifier(), 
                                                             null, 
                                                             declaringKeywords);
      
      if (capturedVars.contains(varStatement)) {
        throw new FormationException(fileName, 
                                     "Repeated captured variable", 
                                     varIdentifier.getLine(), 
                                     varIdentifier.getColumn());
      }
      else {
        capturedVars.add(varStatement);
      }
    }
    
    actualNodes.add(new CaptureStatement(captureKeyword.getLine(), captureKeyword.getColumn(), capturedVars));
    
    return node;
  }
  
  @Override
  protected void enterStatement(Production node) throws ParseException {
    setEntrance();
    //System.out.println("-------->ENTER STATMENT: "+actualNodes);
  }
  
  @Override
  protected Node exitStatement(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    //System.out.println("------------> EXIT STATEMENT: "+exprs);
    
    ASTNode firstExpr = exprs.pollFirst();
    
    if(firstExpr instanceof Keyword){
      Keyword modifier = (Keyword) firstExpr;    
      
      if(exprs.isEmpty()) {
        //keyword statement, like break or continue or return
        if(modifier.getKeyWord() == ReservedWords.RETURN) {
          NullValue nullValue = new NullValue(-1, -1);
          ExpressionStatement nullReturn = new ExpressionStatement(modifier.getLine(), modifier.getColumn(), nullValue, ReservedWords.RETURN);
          actualNodes.push(nullReturn);
        }
        else {
          KeywordStatement statement = new KeywordStatement(modifier.getLine(), modifier.getColumn(), modifier);
          actualNodes.push(statement);
        }
      }
      else {
        //this is an expression statement with a keyword declaration - like a return statement
        ExpressionStatement statement = new ExpressionStatement(modifier.getLine(), 
                                                                modifier.getColumn(), 
                                                                exprs.pollFirst(), 
                                                                modifier.getKeyWord());
        actualNodes.push(statement);
      }
    }
    else {
      actualNodes.push(new ExpressionStatement(firstExpr.getLine(), firstExpr.getColumn(), firstExpr, null));
      //System.out.println("**********UNKNOWN CASE: "+firstExpr+" | "+firstExpr.getClass().getName());
    }
    return node;
  }
  
  @Override
  protected void enterUnit(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitUnit(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
        
    //System.out.println("UNIT EXIT: "+exprs);
    
    ASTNode current = exprs.pollFirst();
    
    /*
    if (current instanceof Keyword && ((Keyword) current).getKeyWord() == ReservedWords.NEW) {
      instanciate = true;
      current = exprs.pollFirst();
    }
    */
    
    while (!exprs.isEmpty()) {
      ASTNode suffix = exprs.pollFirst();
      if (suffix instanceof FunctionCall) {
        FunctionCall fcSuff = (FunctionCall) suffix;
        
        current = new FunctionCall(current.getLine(), current.getColumn(), current, fcSuff.getArguments());
      }
      else if (suffix instanceof ArrayAccess) {
        ArrayAccess aaSuff = (ArrayAccess) suffix;
        
        current = new ArrayAccess(aaSuff.getLine(), aaSuff.getColumn(), current, aaSuff.getIndexValue());
      }
      else if (suffix instanceof AttrAccess) {
        AttrAccess attrSuff = (AttrAccess) suffix;
                
        current = new AttrAccess(current.getLine(), current.getColumn(), current, attrSuff.getAttrName());
      }
    }
    
    if (current instanceof FunctionCall) {
      FunctionCall cur = (FunctionCall) current;
      current = new FunctionCall(cur.getLine(), cur.getColumn(), cur.getTarget(), cur.getArguments());
    }
    
    actualNodes.push(current);
    
    return node;
  }
  
  @Override
  protected void enterExpr(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitExpr(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();

    //System.out.println("EXPR: "+exprs+" || "+actualNodes);
    
    ASTNode currNode = exprs.pollFirst();
    
    while (!exprs.isEmpty()) {
      if (exprs.peekFirst() instanceof Operator) {
        //this is a binary operation
        Operator opNode = (Operator) exprs.pollFirst();
        
        if(OperatorKind.operatorMutatesLeft(opNode.getOp()) && !currNode.isLValue()){
          throw new FormationException(fileName, 
                                       "Left side of expression must be an assignable expression - an l-val.", 
                                       opNode.getLine(), 
                                       opNode.getColumn());
        }
        
        ASTNode righNode = exprs.pollFirst();
        currNode = new BinaryOpExpr(currNode.getLine(), currNode.getColumn(), opNode.getOp(), currNode, righNode); 
      }
    }

    actualNodes.push(currNode);
    
    return node;
  }
  
  @Override
  protected void enterUnary(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitUnary(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println("UNARY EXPR: "+exprs+" || "+actualNodes);
    
    if (exprs.peekFirst() instanceof Operator) {
      Operator opNode = (Operator) exprs.pollFirst();
      ASTNode targetNode = exprs.pollFirst();
      actualNodes.push(new Unary(opNode.getLine(), opNode.getColumn(), opNode.getOp(), targetNode));
    }    
    else {
      actualNodes.push(exprs.pollFirst());
    }
    
    return node;
  }
  
  @Override
  protected void enterScopeBlock(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitScopeBlock(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    Keyword curlyBrace = (Keyword) exprs.pollFirst();
    
    ArrayList<Statement> block = new ArrayList<>();
    while (!exprs.isEmpty()) {
      block.add((Statement) exprs.pollFirst());
    }
    
    ScopeBlock scopeBlock = new ScopeBlock(curlyBrace.getLine(), 
                                 curlyBrace.getColumn(), 
                                 curlyBrace, 
                                 block);
    actualNodes.push(scopeBlock);
    
    return node;
  }
  
  @Override
  protected void enterIfElse(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitIfElse(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println(">>>>>>>>>>>IF ELSE: "+exprs);
    
    //Poll "if" keyword
    Keyword ifKeyword = (Keyword) exprs.pollFirst();
    ASTNode condExpr = exprs.pollFirst();

    //ignore the opening curly brace
    exprs.pollFirst();
    
    ArrayList<Statement> ifBody = new ArrayList<>();
    
    while (!exprs.isEmpty() && !(exprs.peekFirst() instanceof Keyword)) {
      ifBody.add((Statement) exprs.pollFirst());
    }
    
    //System.out.println("  --- IF STATEMENTS: "+ifBody);
    
    ArrayList<IfElse> branches = new ArrayList<>();
    
    while (!exprs.isEmpty()) {
      //Poll "if" keyword
      Keyword keyword = (Keyword) exprs.pollFirst();
      ASTNode nextedCondExpr = null;
      
      if (keyword.getKeyWord() == ReservedWords.ELIF) {
        nextedCondExpr = exprs.pollFirst();
      }
      
      //ignore the opening curly brace
      exprs.pollFirst();
      
      ArrayList<Statement> body = new ArrayList<>();
      while (!exprs.isEmpty() && !(exprs.peekFirst() instanceof Keyword)) {
        body.add((Statement) exprs.pollFirst());
      }
      
      branches.add(new IfElse(keyword, nextedCondExpr, body, new ArrayList<>()));
    }
    
    actualNodes.push(new IfElse(ifKeyword, condExpr, ifBody, branches));
    
    return node;
  }
  
  @Override
  protected void enterWhileLoop(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitWhileLoop(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //poll "while" keyword
    Keyword whileKeyword = (Keyword) exprs.pollFirst();
    
    ASTNode condExpr = exprs.pollFirst();
    
    //ignore the opening curly brace
    exprs.pollFirst();
    
    ArrayList<Statement> loopBody = new ArrayList<>();
    
    while (!exprs.isEmpty()) {
      loopBody.add((Statement) exprs.pollFirst());
    }
    
    WhileLoop loop = new WhileLoop(whileKeyword, condExpr, loopBody);
    actualNodes.push(loop);
    
    return node;
  }
  
  @Override
  protected void enterErrorHandling(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitErrorHandling(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();

    //poll "try" keyword
    Keyword tryKeyword = (Keyword) exprs.pollFirst();
    
    //ignore opening curly brace
    exprs.pollFirst();
    
    ArrayList<Statement> errorBlock = new ArrayList<>();
    
    while (!(exprs.peekFirst() instanceof Keyword)) {
      errorBlock.add((Statement) exprs.pollFirst());
    }
    
    //ignore catch keyword
    exprs.pollFirst();
    
    Identifier erroHandle = (Identifier) exprs.pollFirst();
    
    //ignore opening curly brace for catch
    exprs.pollFirst();
    
    ArrayList<Statement> handleBlock = new ArrayList<>();
    
    while (!exprs.isEmpty()) {
      //System.out.println(" -> CATCH VALUE: "+exprs.peekFirst());
      handleBlock.add((Statement) exprs.pollFirst());
    }
    
    TryCatch tryCatch = new TryCatch(tryKeyword, errorBlock, erroHandle, handleBlock);
    actualNodes.push(tryCatch);
    
    return node;
  }
  
  @Override
  protected void enterDataDefinition(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitDataDefinition(Production node) throws ParseException {
    
    /*
     * A data definition is a syntactic sugar for a way for structuring
     * the construction of an object.
     * 
     * Internally, a data definition is a function.
     */
    
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    HashSet<Keyword> modifiers = new HashSet<>();
    
    while (!(exprs.peekFirst() instanceof Identifier)) {
      Keyword currKeyword = (Keyword) exprs.pollFirst();
      
      if (currKeyword.getKeyWord() != ReservedWords.DATA) {
        modifiers.add(currKeyword);
      }
    }
    
    
    Identifier className = (Identifier) exprs.pollFirst();
    
    //remove opening curly brace
    exprs.pollFirst();
    
    //Keeps track of class functions/methods for DataDefinition
    LinkedHashMap<String, ASTNode> methods = new LinkedHashMap<>();
        
    FuncDef constructor = null;
        
    while (!exprs.isEmpty()) {
      FuncDef method = (FuncDef) exprs.pollFirst();
      
      if (methods.containsKey(method.getBoundName())) {
        throw new FormationException(fileName, 
                                     "The method '"+method.getBoundName()+"' is already defined!", 
                                     method.getLine(), 
                                     method.getColumn());
      }
      
      //Add appropriate captured variables to this method
      method = new FuncDef(method.getLine(), 
                           method.getColumn(), 
                           method.getBoundName(),
                           method.getSignature(), 
                           new LinkedHashSet<>(), 
                           method.getParams(), 
                           method.getStatements());
      
      if (method.getBoundName().equals(ReservedWords.CONSTR.actualWord)) {
        constructor = method;
      }
      
      methods.put(method.getBoundName(), method);
    }
    
    //add a no-arg constructor if no constructor was defined
    if (constructor == null) {
      
      constructor = new FuncDef(-1, 
                                  -1, 
                                  ReservedWords.CONSTR.actualWord,
                                  new FunctionSignature(0, new HashSet<>()),
                                  new LinkedHashSet<>(),
                                  new LinkedHashMap<>(),
                                  new ArrayList<>());
      
      methods.put(constructor.getBoundName(), constructor);
    }
    
    //Create the function dictionary
    ObjectLiteral dict = new ObjectLiteral(className.getLine(), className.getColumn(), methods);
    ExpressionStatement returnStatement = new ExpressionStatement(className.getLine(), className.getColumn(), dict, ReservedWords.RETURN);
    ArrayList<Statement> funcStatements = new ArrayList<>(Arrays.asList(returnStatement));
    
    FuncDef actualDataDef = new FuncDef(className.getLine(), 
                                        className.getColumn(), 
                                        className.getIdentifier(), 
                                        constructor.getSignature(), 
                                        new LinkedHashSet<>(), 
                                        constructor.getParams(), 
                                        funcStatements);
    
    VariableStatement variableStatement = new VariableStatement(className.getLine(), 
                                                                className.getColumn(), 
                                                                className.getIdentifier(), 
                                                                actualDataDef, 
                                                                modifiers.toArray(new Keyword[modifiers.size()]));
        
    actualNodes.push(variableStatement);
    
    return node;
  }
  
  @Override
  protected void enterFunctionBody(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitFunctionBody(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    HashSet<Keyword> modifiers = new HashSet<>();
    
    while (!(exprs.peekFirst() instanceof Identifier)) {
      Keyword currKeyword = (Keyword) exprs.pollFirst();
      if(currKeyword.getKeyWord() != ReservedWords.FUNC) {
        modifiers.add(currKeyword);
      }
    }
        
    Identifier funcName = (Identifier) exprs.pollFirst();
    
    FuncSigInfo signature = formSignature(funcName.getIdentifier(), exprs, modifiers);
    
    //System.out.println("--------FUNC BODY!!! "+funcName+" || "+signature.allParams);
    
    //skip open curly braces
    exprs.pollFirst();
    
    ArrayList<Statement> stmts = new ArrayList<>();
    while (!exprs.isEmpty()) {
      stmts.add((Statement) exprs.pollFirst());
    }
        
    FuncDef def = new FuncDef(funcName.getLine(), 
                              funcName.getColumn(), 
                              funcName.getIdentifier(),
                              signature.signature,
                              new LinkedHashSet<>(), 
                              signature.allParams,
                              stmts);
    
    VariableStatement statement = new VariableStatement(funcName.getLine(), 
                                                        funcName.getColumn(), 
                                                        funcName.getIdentifier(), 
                                                        def, 
                                                        new Keyword[modifiers.size()]);
    
    actualNodes.push(statement);
    return node;
  }
  
  @Override
  protected void enterConstructorBody(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitConstructorBody(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    Keyword funcKeyword = (Keyword) exprs.pollFirst();
    
    FuncSigInfo signature = formSignature(funcKeyword.getKeyWord().actualWord, exprs, new HashSet<>());
    
    //skip open curly braces
    exprs.pollFirst();
    
    ArrayList<Statement> stmts = new ArrayList<>();
    while (!exprs.isEmpty()) {
      stmts.add((Statement) exprs.pollFirst());
    }
        
    FuncDef def = new FuncDef(funcKeyword.getLine(), 
                              funcKeyword.getColumn(), 
                              funcKeyword.getKeyWord().actualWord, 
                              signature.signature,
                              new LinkedHashSet<>(), 
                              signature.allParams,
                              stmts);
    actualNodes.push(def);
    return node;
  }
  
  @Override
  protected void enterArrayLiteral(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitArrayLiteral(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    Keyword openingSquareBrace = (Keyword) exprs.pollFirst();
    ASTNode [] arrValues = new ASTNode[exprs.size()];
    
    //System.out.println("ARRAY LITERAL: "+exprs+" || "+actualNodes);
    
    for(int i = 0; i < arrValues.length; i++) {
      arrValues[i] = exprs.pollFirst();
    }
    
    actualNodes.push(new ArrayLiteral(openingSquareBrace.getLine(), openingSquareBrace.getColumn(), arrValues));
    
    return node;
  }
  
  @Override
  protected void enterArrayAcc(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitArrayAcc(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    Keyword openingSquareBrace = (Keyword) exprs.pollFirst();
    
    ASTNode indexExpr = exprs.pollFirst();
    
    //System.out.println("ARRAY ACCESS: "+exprs+" || "+actualNodes);
    
    actualNodes.push(new ArrayAccess(openingSquareBrace.getLine(), openingSquareBrace.getColumn(), null, indexExpr));
    
    return node;
  }
  
  @Override
  protected void enterDictionary(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitDictionary(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    Keyword openingCurlyBrace = (Keyword) exprs.pollFirst();
    
    LinkedHashMap<String, ASTNode> keyVals = new LinkedHashMap<>();
    while (!exprs.isEmpty()) {
      
      String key = null;
      ASTNode keyNode = exprs.pollFirst();
      if(keyNode instanceof Identifier) {
        key = ((Identifier) keyNode).getIdentifier();
      }
      else {
        key = ((Str) keyNode).getValue();
      }
      
      ASTNode value = exprs.pollFirst();

      if(keyVals.containsKey(key)) {
        throw new FormationException(fileName, 
                                     "Dictionary key '"+key+"' is already defined!", 
                                     keyNode.getLine(), 
                                     keyNode.getColumn());
      }
      
      keyVals.put(key, value);
    }
    
    actualNodes.push(new ObjectLiteral(openingCurlyBrace.getLine(), openingCurlyBrace.getColumn(), keyVals));
    
    return node;
  }
  
  @Override
  protected void enterCallArg(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitCallArg(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println("CALL ARG: "+exprs+" || "+actualNodes);
    
    if (exprs.size() == 1) {
      //this is a normal argument. No special assignment
      actualNodes.push(new CallArg(exprs.pollFirst()));
    }
    else {
      Identifier optionalArgName = (Identifier) exprs.pollFirst();
      
      //ignore equal sign
      exprs.pollFirst();
      
      ASTNode initValue = exprs.pollFirst();
      actualNodes.push(new CallArg(optionalArgName.getIdentifier(), initValue));
    }
    
    return node;
  }
  
  @Override
  protected void enterParameter(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitParameter(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    boolean isConst = false;
    if(exprs.peekFirst() instanceof Keyword) {
      Keyword constKeyword = (Keyword) exprs.pollFirst();
      isConst = constKeyword.getKeyWord() == ReservedWords.CONST;
    }
    
    boolean isVariableParam = false;
    if(exprs.peekFirst() instanceof Operator) {
      Operator lessThanOperator = (Operator) exprs.pollFirst();
      isVariableParam = lessThanOperator.getOp() == OperatorKind.GREAT;
    }
    
    Identifier identifier = (Identifier) exprs.pollFirst();
    
    if (!exprs.isEmpty()) {
      //discard equal operator
      exprs.pollFirst();
      
      if(isVariableParam) {
        throw new FormationException(fileName, 
                                     "Variable-length parameters cannot be keyword parameters.", 
                                     identifier.getLine(), 
                                     identifier.getColumn());
      }
      
      ASTNode initValue = exprs.pollFirst();
      Parameter parameter = new Parameter(identifier.getLine(), 
                                          identifier.getColumn(), 
                                          identifier.getIdentifier(), 
                                          initValue,
                                          isConst,
                                          isVariableParam);
      actualNodes.push(parameter);
    }
    else {
      Parameter parameter = new Parameter(identifier.getLine(), 
          identifier.getColumn(), 
          identifier.getIdentifier(), 
          isConst,
          isVariableParam);
      actualNodes.push(parameter);
    }
    
    return node;
  }
  
  @Override
  protected void enterFuncCall(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitFuncCall(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    //System.out.println("FUNC CALL: "+exprs+" || "+actualNodes);
    
    CallArg [] args = new CallArg[exprs.size()];
    
    /*
     * Argument structure:
     * 
     * positional, keyword, and the rest are variable
     */
    
    boolean expectKeywordArg = true;
    boolean expectPositionArg = true;
    boolean expectVariable = true;
    
    for (int i = 0; i < args.length; i++) {
      args[i] = (CallArg) exprs.pollFirst();
      
      if (args[i].isKeywordArg()) {
        if (expectKeywordArg) {
          expectPositionArg = false;
          expectVariable = true;
          expectKeywordArg = true;
        }
        else {
          throw new FormationException(fileName, 
              "Keyword arguments must be after all positional arguments", 
              args[i].getLine(), 
              args[i].getColumn());
        }
      }
      else {
        if (expectPositionArg) {
          expectPositionArg = true;
          expectKeywordArg = true;
          expectVariable = true;
        }
        else if (expectVariable) {
          expectKeywordArg = false;
          expectPositionArg = false;
          expectVariable = true;
        }
        else {
          throw new FormationException(fileName, 
              "Positional arguments must be before any keyword arguments", 
              args[i].getLine(), 
              args[i].getColumn());
        }
      }
    }
    
    actualNodes.push(new FunctionCall(-1, -1, null, args));
    
    return node;
  }
  
  @Override
  protected void enterAnonFunc(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitAnonFunc(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    //System.out.println("-----ANAON FUNC: "+exprs);
        
    Keyword funcKeyword = (Keyword) exprs.pollFirst();
    
    String boundName = null;
    if (exprs.peekFirst() instanceof Identifier) {
      boundName = ((Identifier) exprs.pollFirst()).getIdentifier();
    }
    
    //System.out.println("-----ANAON FUNC AFTER FUNC: "+exprs);
    
    FuncSigInfo sigInfo = formSignature(ANON_FUNC_NAME, exprs, new HashSet<>());
    
    //skip open curly braces
    exprs.pollFirst();
    
    //Now, gather all capture statements
    LinkedHashSet<VariableStatement> capturedIdents = new LinkedHashSet<>();
    
    while (exprs.peekFirst() instanceof CaptureStatement) {
      CaptureStatement captureStatement = (CaptureStatement) exprs.pollFirst();
      capturedIdents.addAll(captureStatement.getIdentifiers());
    }
    
    //Finally, gather the rest of the statements
    ArrayList<Statement> bodyStatements = new ArrayList<>();
    while (!exprs.isEmpty()) {
      bodyStatements.add((Statement) exprs.pollFirst());
    }
    
    actualNodes.push(new FuncDef(funcKeyword.getLine(), 
                                  funcKeyword.getColumn(), 
                                  boundName, sigInfo.signature, 
                                  capturedIdents, 
                                  sigInfo.allParams, 
                                  bodyStatements));
    
    return node;
  }
  
  @Override
  protected void enterParenExpr(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitParenExpr(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    ASTNode parenExpr = exprs.pollFirst();
    
    actualNodes.push(new Parenthesized(parenExpr.getLine(), parenExpr.getColumn(), parenExpr));
    
    return node;
  }
  
  @Override
  protected void enterAttrLookup(Production node) throws ParseException {
    setEntrance();
  }
  
  @Override
  protected Node exitAttrLookup(Production node) throws ParseException {
    ArrayDeque<ASTNode> exprs = exitEntrance();
    
    ASTNode attrNameNode = exprs.pollFirst(); 
    String attrName = null;
    
    if (attrNameNode instanceof Identifier) {
      attrName = ((Identifier) attrNameNode).getIdentifier();
    }
    else {
      attrName = ((Str) attrNameNode).getValue();
    }
    
    actualNodes.push(new AttrAccess(-1, -1, null, attrName));
    return node;
  }
  
  //HELPER methods start
  private void setEntrance(){
    stack.add(new ArrayDeque<>());
    actualNodes.push(null); //add marker
  }

  private ArrayDeque<ASTNode> exitEntrance(){
    ArrayDeque<ASTNode> latest = stack.pop();

    while (actualNodes.peek() != null) {
      latest.addFirst(actualNodes.pop());
    }

    actualNodes.pop(); //removes marker

    return latest;
  }
  
  //Function Signature parsing stuff--------------
  
  private static class FuncSigInfo {
    private final FunctionSignature signature;
    private final LinkedHashMap<String, Parameter> allParams;
    
    public FuncSigInfo(FunctionSignature signature, LinkedHashMap<String, Parameter> allParams) {
      // TODO Auto-generated constructor stub
      this.signature = signature;
      this.allParams = allParams;
    }
  }
  
  private FuncSigInfo formSignature(String name, ArrayDeque<ASTNode> exprs, HashSet<Keyword> modifiers) {
    
    /*
     * Structure of parameters:
     * 
     * positional, keyword (a.k.a default), variable (only one)
     */
    
    LinkedHashMap<String, Parameter> allParamNames = new LinkedHashMap<>();
    
    int positionalCount = 0;
    LinkedHashMap<String, Parameter> keywordParams = new LinkedHashMap<>();
    boolean hasVariableParam = false;
    
    /*
     * The three boolean flags below are used for parameter placement checking
     */
    boolean expectPositionalNext = true;
    boolean expectKeywordNext = true;
    boolean expectVariable = true;
    
    while (!(exprs.peekFirst() instanceof Keyword)) {
      Parameter parameter = (Parameter) exprs.pollFirst();

      if(allParamNames.containsKey(parameter.getName())) {
        throw new FormationException(fileName, 
            "Parameter name is already in use!", 
            parameter.getLine(),                    
            parameter.getColumn());
      }
      else {
        allParamNames.put(parameter.getName(), parameter);
      }
      
      if (parameter.isAKeywordParameter()) {
        if(expectKeywordNext) {
          keywordParams.put(parameter.getName(), parameter);
          
          expectPositionalNext = false;
          expectKeywordNext = true;
          expectVariable = true;
        }
        else {
          throw new FormationException(fileName, 
              "Keyword paramters must be declared after all positional paramters.", 
              parameter.getLine(), 
              parameter.getColumn());
        }
      }
      else if (parameter.isVariableParam()) {
        if(expectVariable) {
          hasVariableParam = true;
          
          expectPositionalNext = false;
          expectKeywordNext = false;
          expectVariable = false;
        }
        else {
          throw new FormationException(fileName, 
              "There can only be one variable parameter and it must be the last parameter declared", 
              parameter.getLine(), 
              parameter.getColumn());
        }
      }
      else {
        if(expectPositionalNext) {
          positionalCount++;

          expectPositionalNext = true;
          expectKeywordNext = true;
          expectVariable = true;
        }
        else {
          throw new FormationException(fileName, 
              "Positional parameters must preceed keyword and variable parameters", 
              parameter.getLine(), 
              parameter.getColumn());
        }

      }
    }
    
    return new FuncSigInfo(
        new FunctionSignature(positionalCount, 
                              keywordParams.keySet()), 
        allParamNames);
  }
  //HELPER methods DONE
  
  @Override
  public void reset() {
    stack.clear();
    actualNodes.clear();
    rawModule = null;
    fileName = null;
  }
  
  public Module getModule(){
    return rawModule;
  }
}
