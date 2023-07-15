package jg.sh.parsing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.parsing.exceptions.ParseException;
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
import jg.sh.parsing.nodes.FuncCall.Argument;
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
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;
import jg.sh.parsing.token.Token;
import jg.sh.parsing.token.TokenType;

import static jg.sh.parsing.token.TokenType.*;

public class Parser {

  private static Logger LOG = LogManager.getLogger(Parser.class);

  private TokenizerIterator tokenStream;
  private String moduleName;

  public Parser(Tokenizer tokenizer, String moduleName) {
    reset(tokenizer, moduleName);
  }

  public Parser(List<Token> tokens, String moduleName) {
    reset(tokens, moduleName);
  }

  public Parser reset(Tokenizer tokenizer, String moduleName) {
    this.tokenStream = tokenizer.iterator();
    this.moduleName = moduleName;
    return this;
  }

  public Parser reset(List<Token> tokens, String moduleName) {
    this.tokenStream = new TokenizerIterator(tokens);
    this.moduleName = moduleName;
    return this;
  }

  // parsing methods - START

  public Module parseProgram() throws ParseException {
    final List<UseStatement> useStatements = new ArrayList<>();
    final Set<String> takenSymbols = new HashSet<>();

    final List<Statement> statements = new ArrayList<>();

    LOG.info("---parsing program ");

    while (hasNext()) {
      if (match(TokenType.EOF)) {
        /*
         * Why do we check for EOF first?
         * 
         * In the case of an empty program/file, at the first call to this method (parseProgram()),
         * Tokenizer is still optimistic in that it still thinks there's a non-EOF token
         * to be tokenizer (meaning hasNext() returns true).
         * 
         * When we call the first match(), that's when we first ask Tokenizer to tokenize its
         * first Token. Since it's an empty program/file, there's no Token to tokenize! It reached
         * the end of the file already! so the first Token it gives is an EOF after
         * that first match call.
         * 
         * Subsequent match calls now will automically return false and not even do any checking
         * because we've already received the last Token (EOF)
         * 
         * So we check for EOF first to avoid this problem.
         */
        break;
      }
      else if (match(TokenType.USE)) {
        final UseStatement useStatement = useStatement(prev());
        useStatements.add(useStatement);
      }
      else if (match(TokenType.FUNC)) {
        final FuncDef funcDef = funcDef(prev(), true, true);
        if (takenSymbols.contains(funcDef.getBoundName().getIdentifier())) {
          throw new ParseException("'"+funcDef.getBoundName().getIdentifier()+"' is already a top-level symbol.", 
                                   funcDef.getBoundName().end, moduleName);
        } 
        else {
          takenSymbols.add(funcDef.getBoundName().getIdentifier());
          statements.add(new Statement(funcDef, funcDef.start, funcDef.end));
        }
      } 
      else if (match(TokenType.DATA)) {
        final DataDefinition dataDef = dataDefinition(prev());
        if (takenSymbols.contains(dataDef.getName().getIdentifier())) {
          throw new IllegalStateException("The top-level symbol '" + dataDef.getName() + "' is already taken");
        } 
        else {
          takenSymbols.add(dataDef.getName().getIdentifier());
          statements.add(dataDef);
        }
      } 
      else if (match(TokenType.CONST)) {
        final Node varOrConstAttr = varDeclrOrConstAttr(prev());

        if (varOrConstAttr instanceof VarDeclrList) {
          final VarDeclrList varDeclrs = (VarDeclrList) varOrConstAttr;

          for (VarDeclr modVar : varDeclrs.getVarDeclrs()) {
            if (takenSymbols.contains(modVar.getName().getIdentifier())) {
              throw new IllegalStateException("The top-level symbol '" + modVar.getName().getIdentifier() + "' is already taken");
            } 
            else {
              takenSymbols.add(modVar.getName().getIdentifier());
              statements.add(modVar);
            } 
          }
        }
        else {
          //This is a const attr statement
          statements.add(new Statement((ConstAttrDeclr) varOrConstAttr, varOrConstAttr.start, varOrConstAttr.end));
        }
      }
      else if (match(TokenType.VAR)) {
        final VarDeclrList varDeclrs = varDeclrs(prev());

        for (VarDeclr modVar : varDeclrs.getVarDeclrs()) {
          if (takenSymbols.contains(modVar.getName().getIdentifier())) {
            throw new IllegalStateException("The top-level symbol '" + modVar.getName().getIdentifier() + "' is already taken");
          } 
          else {
            takenSymbols.add(modVar.getName().getIdentifier());
            statements.add(modVar);
          } 
        }
      } 
      else {
        //LOG.info("  ==> top level peek? "+peek());

        final Statement topLevelStatement = statement();
        statements.add(topLevelStatement);
        /*
        LOG.info("---else: "+prev().getType());
        final Token unknown = peek();
        throw new ParseException("Unknown token '" + unknown.getContent() + "' at top level.", 
                                 unknown.getStart(), 
                                 unknown.getEnd());
        */
      }
    }

    LOG.info(peek() + "<--- last");

    final Module program = new Module(moduleName, useStatements, statements);
    return program;
  }

  /*
   * Use declarations are of the following format:
   *      use <module name> [as <alias>];
   * or
   *      use <module name>::<module component name> (, <module component name>)* ;
   * 
   * Example: 
   *      use ModuleOne;  //Imports the module named "ModuleOne"
   *      use ModuleOne:funky; //Imports the module component from "ModuleOne" called "funky", but doesn't import ModuleOne
   *      use ModuleOne:funky, boo, bar; //Imports the funky, boo, and bar components from ModuleOne
   * 
   * Alias - Use declaration can append an alias as to not conflict with any equally-named component
   *         in the module:
   *  
   *      use ModuleOne as Mod; //ModuleOne now has the alias of "Mod" in the module
   *      use ModuleOne:funky as func; //funky can be now be used as "func"
   *      use ModuleOne:funky as fy, boo as apple; //funky can be used as "fy" and boo as "apple"
   */
  private UseStatement useStatement(Token useKeyword) throws ParseException {
    final Token moduleName = matchError(IDENTIFIER, "Module name expected.", useKeyword.getEnd());

    final Identifier importedModule = new Identifier(moduleName);
    Identifier importedModuleAlias = null;

    final Map<Identifier, Identifier> compAliasMap = new HashMap<>();

    if (match(AS)) {
      //Imported module has an alias. 
      final Token moduleAlias = matchError(IDENTIFIER, "Module alias expected.", prev().getEnd());
      importedModuleAlias = new Identifier(moduleAlias);
    }
    else if(match(COLON)) {
      do {
        final Token compName = matchError(IDENTIFIER, "Component name expected.", prev().getEnd());

        if (match(AS)) {
          final Token compAlias = matchError(IDENTIFIER, "Component alias expected.", prev().getEnd());
          compAliasMap.put(new Identifier(compName), new Identifier(compAlias));
        }
        else {
          compAliasMap.put(new Identifier(compName), null);
        }
      } while (match(COMMA));
    } 

    final Token semicolon = matchError(SEMICOLON, "';' expected.", prev().getEnd());

    return new UseStatement(importedModule, importedModuleAlias, compAliasMap, useKeyword.getStart(), semicolon.getEnd());
  }

  /**
   * A function definition.
   * 
   * Top-level format:
   * 
   * func [export] <func_name> ([parameter,...]) {
   *    //function body
   * }
   * 
   * Expression-level format:
   * 
   * func [<boundName>] ([parameter,...]) {
   *  //function body
   * }
   * 
   * Note: boundName is required if recursion is needed for that function
   * 
   * where the format of paramter is:
   *   parameter = identifier | identifier := expr
   * 
   * The first statement of a definition can be a capture statement - and only the first statement.
   */
  private FuncDef funcDef(Token keyword, boolean isTopLevel, boolean requireName) throws ParseException {
    final boolean toExport = isTopLevel? match(EXPORT) : false;
    final Token name = isTopLevel || requireName ? 
                          matchError(IDENTIFIER, "Function name expected.", prev().getEnd()) :
                         (match(IDENTIFIER) ? prev() : null);

    matchError(LEFT_PAREN, "'(' expected.", name != null ? name.getEnd() : keyword.getEnd());

    final LinkedHashMap<String, Parameter> paramMap = new LinkedHashMap<>();
    final HashSet<String> optionalParams = new HashSet<>();

    boolean hasVarParam = false;
    int positionalCount = 0;
    boolean hasVarKeywordParams = false;

    if (!match(RIGHT_PAREN)) {
      do {
        final Parameter parameter = parameter();
        if (paramMap.containsKey(parameter.getName().getIdentifier())) {
          throw new ParseException("'"+parameter.getName().getIdentifier()+"' is already a parameter.", 
                                   parameter.getName().start, 
                                   parameter.getName().end,
                                   moduleName);
        }
        paramMap.put(parameter.getName().getIdentifier(), parameter);

        if (parameter.isVarying()) {
          /**
           * The varying parameter is the last parameter.
           */
          hasVarParam = true;
          break;
        }
        else if(parameter.isVarArgsKeyword()) {
          hasVarKeywordParams = true;
        }
        else if(!parameter.hasValue()) {
          positionalCount++;
        }
        else {
          optionalParams.add(parameter.getName().getIdentifier());
        }
      } while(match(COMMA));

      matchError(RIGHT_PAREN, "')' expected.", prev().getEnd());
    }

    //LOG.info("params so far: "+paramMap.values());
    //LOG.info("=====> funcDef after rightParent: "+prev()+" | peeked: "+peek());

    final Token leftCurly = matchError(LEFT_CURL, "'{' expected", prev().getEnd());
    final Block funcBlock = block(leftCurly);

    final CaptureStatement captureStatement = funcBlock.size() > 0 && funcBlock.get(0) instanceof CaptureStatement ? 
                                                (CaptureStatement) funcBlock.getStatements().get(0) : 
                                                null;

    LOG.info("=== FUNC STATEMENTS: "+captureStatement);
    LOG.info(funcBlock);

    if (captureStatement != null) {
      //Remove capture statement from body
      funcBlock.getStatements().remove(0);
    }

    return new FuncDef(name != null ? new Identifier(name) : null, 
                       new FunctionSignature(positionalCount, optionalParams, hasVarParam, hasVarKeywordParams),
                       captureStatement != null ? captureStatement.getCaptures() : Collections.emptySet(), 
                       paramMap, 
                       toExport, 
                       funcBlock, 
                       keyword.getStart(), 
                       funcBlock.getEnd());
  }

  /**
   * Format:
   * 
   * data [export] <dataTypeName {
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
   */
  private DataDefinition dataDefinition(Token dataKeyword) throws ParseException {
    boolean toExport = false, isSealed = false;

    /**
     * These if statements make sure that there's no duplicate
     * export and/or sealed keywords
     */
    if (match(EXPORT)) {
      toExport = true;
      if (match(SEALED)) {
        isSealed = true;
      }
    }
    else if (match(SEALED)) {
      isSealed = true;
      if(match(EXPORT)) {
        toExport = true;
      }
    }

    final Token dataTypeName = matchError(IDENTIFIER, null, toExport ? prev().getEnd() : dataKeyword.getEnd());

    matchError(LEFT_CURL, "'{' expected.", dataTypeName.getEnd());

    final LinkedHashMap<Identifier, FuncDef> methods = new LinkedHashMap<>();

    FuncDef constructor = null;

    while (!match(RIGHT_CURL)) {
      FuncDef method = null;

      if (match(FUNC)) {
        method = funcDef(prev(), false, true);
      }
      else if(match(CONSTR)) {
        constructor = method = contructor(prev());
      }
      else {
        final Token unknown = peek();
        throw new ParseException("Unknown token '"+unknown.getContent()+"'", 
                                 unknown.getStart(), 
                                 unknown.getEnd(), 
                                 moduleName);
      }
      
      methods.put(method.getBoundName(), method);
    }

    if (constructor == null) {
      /*
       * Create no-arg constructor
       */
      final Identifier contrIden = new Identifier(TokenType.CONSTR.name().toLowerCase(), Location.DUMMY, Location.DUMMY);
      constructor = new FuncDef(contrIden, 
                                FunctionSignature.NO_ARG,
                                Collections.emptySet(), 
                                new LinkedHashMap<>(), 
                                false, 
                                new Block(Collections.emptyList(), Location.DUMMY, Location.DUMMY), 
                                Location.DUMMY, Location.DUMMY);
    }

    return new DataDefinition(new Identifier(dataTypeName), constructor, methods, toExport, isSealed, prev().getEnd());
  }

  /**
   * Format:
   * 
   * capture var1, ... ;
   * 
   * where varN is an identifier.
   */
  private CaptureStatement captureStatement(Token captureKeyword) throws ParseException {
    final HashSet<Identifier> captured = new HashSet<>();

    Token recent = captureKeyword;

    do {
      final Token varName = recent = matchError(IDENTIFIER, "Captured variable name expected.", recent.getEnd());
      final Identifier identifier = new Identifier(varName);
      
      if (captured.contains(identifier)) {
        throw new ParseException("'"+varName.getContent()+"' has already been captured.", 
                                 identifier.start, 
                                 identifier.end,
                                 moduleName);
      }
      captured.add(identifier);
    } while(match(COMMA));

    recent = prev();

    final Token semicolon = matchError(SEMICOLON, "';' expected.", prev().getEnd());

    return new CaptureStatement(captured, captureKeyword.getStart(), semicolon.getEnd());
  }

  /**
   * Format:
   * 
   * [const] [var] [!] paramName [:= node]
   * 
   * where node is the initial value of the parameter
   */
  private Parameter parameter() throws ParseException {
    final Set<Keyword> descriptors = new HashSet<>();
    boolean varKeywordArgs = false;

    if (match(CONST)) {
      descriptors.add(new Keyword(prev()));
    }
    if (match(VAR)) {
      descriptors.add(new Keyword(prev()));
    }
    if(match(BANG)) {
      varKeywordArgs = true;
    }

    final Token paramName = matchError(IDENTIFIER, "Parameter name expected.", prev().getEnd());
    final Node initValue = match(ASSIGNMENT) ? expr() : null;

    return new Parameter(new Identifier(paramName), initValue, varKeywordArgs, descriptors);
  }

  /**
   * Constructor syntax:
   * 
   * constr([parameter,...]) {
   * 
   * }
   */
  private FuncDef contructor(Token constrKeyword) throws ParseException {
    matchError(LEFT_PAREN, "'(' expected.", constrKeyword.getEnd());

    final LinkedHashMap<String, Parameter> paramMap = new LinkedHashMap<>();
    boolean hasVarParam = false;
    int positionalCount = 0;
    boolean hasVarKeywordParams = false;

    while (!match(RIGHT_PAREN)) {
      final Parameter parameter = parameter();

      if (paramMap.containsKey(parameter.getName().getIdentifier())) {
        throw new ParseException("'"+parameter.getName().getIdentifier()+"' is already a parameter.", 
                                 parameter.getName().start, 
                                 parameter.getName().end,
                                 moduleName);
      }
      paramMap.put(parameter.getName().getIdentifier(), parameter);

      if (parameter.isVarying()) {
        /**
         * The varying parameter is the last parameter.
         */
        hasVarParam = true;
        break;
      }
      else if(parameter.hasValue()) {
        positionalCount++;
      }

      if (match(COMMA)) {
        continue;
      }
    }

    final Token leftCurly = matchError(LEFT_CURL, "'{' expected", prev().getEnd());
    final Block funcBlock = block(leftCurly);

    final CaptureStatement captureStatement = funcBlock.size() > 0 && funcBlock.get(0) instanceof CaptureStatement ? 
                                                (CaptureStatement) funcBlock.getStatements().get(0) : 
                                                null;

    if (captureStatement != null) {
      //Remove capture statement from body
      funcBlock.getStatements().remove(0);
    }

    return new FuncDef(new Identifier(constrKeyword), 
                       new FunctionSignature(positionalCount, paramMap.keySet(), hasVarParam, hasVarKeywordParams),
                       captureStatement != null ? captureStatement.getCaptures() : null, 
                       paramMap, 
                       false, 
                       funcBlock, 
                       constrKeyword.getStart(), 
                       funcBlock.getEnd());
  }

  /**
   * Parses either a VarDeclrList or a ConstAttrDeclr, or throws a ParseException
   * if neither pattern is matched
   */
  private Node varDeclrOrConstAttr(Token constKeyword) throws ParseException {
    final Token constToken = prev();

    if (match(EXPORT)) {
      final Token exportToken = prev();
      if(match(IDENTIFIER)) {
        //This is a variable declaration
        tokenStream.pushback();
        tokenStream.pushback();
        return varDeclrs(constToken);
      }
      throw new ParseException("Unknown token "+exportToken, exportToken.getStart(), exportToken.getEnd(), moduleName);
    }
    else if(match(IDENTIFIER)) {
      //This is a variable declaration
      tokenStream.pushback();
      return varDeclrs(constToken);
    }
    else {
      final Node target = expr();
      AttrAccess access = null;

      while(match(DOT)) {
        final Token dot = prev();
        access = attrAccess(access == null ? target : access, dot);
      }

      if (access == null) {
        throw new ParseException("Immutable attribute declaration incomplete.", target.start, target.end, moduleName);
      }

      matchError(ASSIGNMENT, "Missing assignment for immutable attribute declaration.", access.end);

      final Node initValue = expr();
      return new ConstAttrDeclr(access, initValue);
    }
  }

  /**
   * Format:
   * 
   * var [export] (varNam [:= expr],)+;
   * 
   * or 
   * 
   * const [export] (varName := expr,)+;
   */
  private VarDeclrList varDeclrs(Token keyword) throws ParseException {
    final HashSet<Keyword> descriptors = new HashSet<>();

    //check if var or const
    if (keyword.getType() == CONST) {
      descriptors.add(new Keyword(keyword));
    }

    //check if exported or not (only top level!)
    if (match(EXPORT)) {
      descriptors.add(new Keyword(prev()));
    }

    final LinkedHashSet<VarDeclr> vars = new LinkedHashSet<>();

    Location recent = keyword.getEnd();

    do {
      final Token varName = matchError(IDENTIFIER, "Captured variable name expected.", recent);
      final Identifier identifier = new Identifier(varName);
      Node initValue = null;

      recent = varName.getEnd();
      
      if (keyword.getType() == CONST) {
        //Constant variables must have an initial value
        matchError(ASSIGNMENT, "Initial value expected for '"+varName.getContent()+"'.", identifier.end);
        initValue = expr();

        recent = initValue.end;
      }
      else if (match(ASSIGNMENT)) {
        initValue = expr();

        recent = initValue.end;
      }


      final VarDeclr var = new VarDeclr(identifier, 
                                        keyword.getType() == CONST, 
                                        initValue, 
                                        varName.getStart(), 
                                        initValue != null ? initValue.end : varName.getEnd(), 
                                        descriptors);

      if (vars.contains(var)) {
        throw new ParseException("'"+varName.getContent()+"' has already been used..", identifier.start, identifier.end, moduleName);
      }

      vars.add(var);
    } while(match(COMMA));

    matchError(SEMICOLON, "';' expected.", recent);

    return new VarDeclrList(vars, keyword.getStart(), recent);
  }

  private Statement statement() throws ParseException {
    //LOG.info(" ====> in statement: "+peek()+" "+prev());

    if (match(FUNC)) {
      final FuncDef funcDef = funcDef(prev(), false, false);
      return new Statement(funcDef);
    }
    else if(match(WHILE, IF, TRY, LEFT_CURL)) {
      return block(prev());
    }
    else if(match(CAPTURE)) {
      return captureStatement(prev());
    }
    else if(match(RETURN)) {
      final Keyword keyword = new Keyword(prev());
      if (match(SEMICOLON)) {
        return new ReturnStatement(keyword, prev().getEnd());
      }

      final Node returnValue = expr();

      final Token semicolon = matchError(SEMICOLON, "';' expected.", returnValue.end);
      return new ReturnStatement(keyword, returnValue, semicolon.getEnd());
    }
    else if(match(THROW)) {
      final Keyword keyword = new Keyword(prev());
      final Node returnValue = expr();
      final Token semicolon = matchError(SEMICOLON, "';' expected.", returnValue.end);
      return new ThrowStatement(keyword, returnValue, semicolon.getEnd());
    }
    else if(match(BREAK, CONTINUE)) {
      final Keyword keyword = new Keyword(prev());
      final Token semicolon = matchError(SEMICOLON, "';' expected.", keyword.end);
      return new Statement(keyword, keyword.start, semicolon.getEnd());
    }
    else if(match(SEMICOLON)) {
      //empty statement. Still valid
      final Token semicolon = prev();
      return new Statement(semicolon.getStart(), semicolon.getEnd());
    }
    else if(match(VAR)) {
      return varDeclrs(prev());
    }
    else if(match(CONST)) {
      final Node node = varDeclrOrConstAttr(prev());
      if (node instanceof VarDeclrList) {
        return (VarDeclrList) node;
      }
      else {
        final ConstAttrDeclr constAttr = (ConstAttrDeclr) node;
        matchError(SEMICOLON, "';' expected.", constAttr.end);
        return new Statement(constAttr, constAttr.start, constAttr.end);
      }
    }
    else {
      final Node expr = expr();
      matchError(SEMICOLON, "';' expected.", expr.end);
      return new Statement(expr, expr.start, expr.end);
    }
  }

  private Block block(Token keyword) throws ParseException {
    switch (keyword.getType()) {
      case WHILE: return whileLoop(keyword);
      case IF: return ifElse(keyword);
      case TRY: return tryCatch(keyword);
      case LEFT_CURL: {
        final ArrayList<Statement> statements = new ArrayList<>();

        while (!match(RIGHT_CURL)) {
          final Statement statement = statement();
          statements.add(statement);
        }

        return new Block(statements, keyword.getStart(), prev().getEnd());
      }
      default: throw new ParseException("Unknown token '"+keyword.getContent()+"' for block signifier.", keyword.getStart(), moduleName);
    }
  }

  /**
   * Format:
   * 
   * while (expr) {
   *  statements....
   * }
   */
  private WhileBlock whileLoop(Token whileKeyword) throws ParseException {
    matchError(LEFT_PAREN, "'(' expected for while conditional.", whileKeyword.getEnd());
    final Node condition = expr();
    final Token rightParen = matchError(RIGHT_PAREN, "')' expected for while conditional.", condition.end);
    final Block whileBlock = block(matchError(LEFT_CURL, "'{' expected for while conditional.", rightParen.getEnd()));
    
    return new WhileBlock(condition, whileBlock.getStatements(), whileKeyword.getStart(), whileBlock.getEnd());
  }

  /**
   * Format:
   * 
   * if (expr) {
   * 
   * }
   * (elif(expr) {} )*
   * [else {}]
   */
  private IfBlock ifElse(Token keyword) throws ParseException {
    matchError(LEFT_PAREN, "'(' expected for conditional.", keyword.getEnd());
    final Node condition = expr();
    final Token rightParen = matchError(RIGHT_PAREN, "')' expected for conditional.", condition.end);
    final Block ifBlock = block(matchError(LEFT_CURL, "'{' expected for conditional.", rightParen.getEnd()));

    final ArrayList<IfBlock> otherBranches = new ArrayList<>();

    while (match(ELIF)) {
      final Token elifKeyword = prev();
      matchError(LEFT_PAREN, "'(' expected for elif conditional.", elifKeyword.getEnd());
      final Node elifCondition = expr();
      final Token elifRightParen = matchError(RIGHT_PAREN, "')' expected for elif conditional.", elifCondition.end);
      final Block elifBlock = block(matchError(LEFT_CURL, "'{' expected for elif conditional.", elifRightParen.getEnd()));

      otherBranches.add(new IfBlock(new Keyword(elifKeyword), 
                                    elifCondition, 
                                    elifBlock.getStatements(), 
                                    Collections.emptyList(), 
                                    elifBlock.getEnd()));
    }

    if (match(ELSE)) {
      final Token elseKeyword = prev();
      final Block elseBlock = block(matchError(LEFT_CURL, "'{' expected for else block.", elseKeyword.getEnd()));
      otherBranches.add(new IfBlock(new Keyword(elseKeyword), 
                                    null,
                                    elseBlock.getStatements(), 
                                    Collections.emptyList(), 
                                    elseBlock.getEnd()));
    }

    final Location ending = otherBranches.size() > 0 ? 
                               otherBranches.get(otherBranches.size() - 1).getEnd() : 
                               ifBlock.getEnd();

    return new IfBlock(new Keyword(keyword), 
                       condition, 
                       ifBlock.getStatements(), 
                       otherBranches, 
                       ending);
  }

  /**
   * Format: 
   * 
   * try {
   *    statements....
   * } 
   * catch e {
   *    statements....
   * }
   */
  private TryCatch tryCatch(Token tryKeyword) throws ParseException {
    final Token tryLeftCurl = matchError(LEFT_CURL, "'{' expected for try-block.", tryKeyword.getEnd());

    final Block tryBlock = block(tryLeftCurl);

    final Token catchKeyword = matchError(CATCH, "'catch' keyword expected for try-catch block.", tryBlock.getEnd());
    final Token exceptionHandler = matchError(IDENTIFIER, "Exception handler expected.", catchKeyword.getEnd());
    final Token catchLeftCurl = matchError(LEFT_CURL, "'{' expected for catch-block.", exceptionHandler.getEnd());

    final Block catchBlock = block(catchLeftCurl);

    return new TryCatch(tryBlock.getStatements(), 
                        catchBlock, 
                        new Identifier(exceptionHandler), 
                        tryKeyword.getEnd(), 
                        catchBlock.getEnd());
  }

  private Node expr() throws ParseException {
    Node result = null;
    Location recent = null;

    LOG.info("** new expr: "+peek());

    if (match(TRUE, FALSE)) {
      final Token boolToken = prev();
      result = new Bool(Boolean.parseBoolean(boolToken.getContent()), boolToken.getStart(), boolToken.getEnd());
      recent = result.end;
    }
    else if (match(INTEGER)) {
      final Token intToken = prev();
      result = new Int(Long.parseLong(intToken.getContent()), intToken.getStart(), intToken.getEnd());
      recent = result.end;
    }
    else if (match(DECIMAL)) {
      final Token floatToken = prev();
      result = new FloatingPoint(Double.parseDouble(floatToken.getContent()), floatToken.getStart(), floatToken.getEnd());
      recent = result.end;
    }
    else if (match(STRING)) {
      final Token strToken = prev();
      result = new Str(strToken.getContent(), strToken.getStart(), strToken.getEnd());
      recent = result.end;
    }
    else if (match(IDENTIFIER)) {
      final Token identifierToken = prev();
      result = new Identifier(identifierToken);
      recent = result.end;
    }
    else if (match(NULL)) {
      final Token nullToken = prev();
      result = new Null(nullToken.getStart(), nullToken.getEnd());
      recent = result.end;
    }
    else if(match(MODULE, SELF)) {
      final Token keyword = prev();
      result = new Keyword(keyword);
      recent = result.end;
    }
    else if (match(LEFT_PAREN)) {
      final Token leftParen = prev();
      final Node inner = expr();
      final Token rightParen = matchError(RIGHT_PAREN, null, inner.end);
      result = new Parenthesized(inner, leftParen.getStart(), rightParen.getEnd());
      recent = result.end;
    }
    else if (match(LEFT_SQ_BR)) {
      result = arrayLiteral(prev());
      recent = result.end;
    }
    else if (match(OBJECT)) {
      result = objectLiteral(prev());
      recent = result.end;
    }
    else if(match(FUNC)) {
      result = funcDef(prev(), false, false);
      recent = result.end;
    }
    else if(match(BANG, MINUS)) {
      final Token unary = prev();
      final Node target = expr();
      result = new UnaryExpr(new Operator(unary), target);
      recent = result.end;
    }
    /* 
    else {
      throw new ParseException("Unkown token '"+peek()+"'", peek().getEnd());
    }
    */

   // LOG.info("===> prior to binop: "+result);

    /**
     * Attribute access, function call and index access is more tightly bound
     * to an expression than binary operators
     */
    while (match(LEFT_PAREN, LEFT_SQ_BR, DOT)) {
      final Token op = prev();
      switch (op.getType()) {
        case LEFT_PAREN: {
          result = funcCall(result, op);
          recent = result.end;
          break;
        }
        case LEFT_SQ_BR: {
          result = indexAccess(result, op);
          recent = result.end;
          break;
        }
        case DOT: {
          result = attrAccess(result, op);
          recent = result.end;
          break;
        } 
        default: 
          //This should never be thrown as we're matching for (, [ and .
          throw new ParseException("Unknown token '"+op+"'.", op.getStart(), op.getEnd(), moduleName);
      }
    }

    LOG.info("  ===>AFTER attr, arrayand call: "+result+" "+recent);

    //Exhaust binary operators
    result = binOpExpr(result);
    recent = result.end;

    //LOG.info(" **END: "+result.repr());
    return result;
  }

  private ArrayLiteral arrayLiteral(Token leftSqBracket) throws ParseException {
    final ArrayList<Node> values = new ArrayList<>();

    while (!match(RIGHT_SQ_BR)) {
      final Node value = expr();
      values.add(value);
      if (match(COMMA)) {
        continue;
      }
    }

    return new ArrayLiteral(leftSqBracket.getStart(), prev().getEnd(), values.toArray(new Node[values.size()]));
  }

  private Node binOpExpr(Node leftOperand) throws ParseException {
    while(match(TokenType.binOps)) {
      final Token op = prev();
      final Operator operator = new Operator(op);

      final Node rightOperand = expr();
      //LOG.info("--left: "+leftOperand+" | --right: "+rightOperand+" | op: "+operator);
      leftOperand = new BinaryOpExpr(leftOperand, rightOperand, operator);
    }

    //LOG.info(" ===> new binopexpr: "+leftOperand.repr());

    return leftOperand;
  }

  private FuncCall funcCall(Node target, Token leftParen) throws ParseException {
    final ArrayList<Argument> arguments = new ArrayList<>();

    while (!match(RIGHT_PAREN)) {
      final Node arg = expr();
      Argument actualArg = null;

      if (match(ASSIGNMENT)) {
        if (!(arg instanceof Identifier)) {
          throw new ParseException("Left hand of assignment must be an identifier.", arg.end, moduleName);
        }

        final Node value = expr();
        actualArg = new Argument((Identifier) arg, value);
      }
      else {
        actualArg = new Argument(arg);
      }

      arguments.add(actualArg);

      if (match(COMMA)) {
        continue;
      }
    }

    return new FuncCall(target, prev().getEnd(), arguments.toArray(new Argument[arguments.size()]));
  }

  private IndexAccess indexAccess(Node target, Token leftSqBracket) throws ParseException {
    final Node indexValue = expr();
    matchError(RIGHT_SQ_BR, "']' expected for index operation.", indexValue.end);
    return new IndexAccess(target, indexValue);
  }

  private AttrAccess attrAccess(Node target, Token dotToken) throws ParseException {
    final Token attrName = matchError(IDENTIFIER, "Attribute name expected after '.' .", dotToken.getEnd());
    return new AttrAccess(target, new Identifier(attrName));
  }

  /**
   * Represents an object literal.
   * 
   * Format:
   * 
   * object [sealed] {
   *   [const] attr1 : value1,
       ....
      }
   */
  private ObjectLiteral objectLiteral(Token objectKeyword) throws ParseException {
    final boolean isSealed = match(SEALED);
    final Map<String, Parameter> attrs = new HashMap<>();

    Location recent = matchError(LEFT_CURL, 
                              "'{' expected for object literal.", 
                              isSealed ? prev().getEnd() : objectKeyword.getEnd()).getEnd();

    while (!match(RIGHT_CURL)) {
      final Keyword isConst = match(CONST) ? new Keyword(prev()) : null;

      recent = isConst != null ? isConst.end : recent;

      final Token attributeName = matchError(IDENTIFIER, "Attribute name expected.", recent);

      if (attrs.containsKey(attributeName.getContent())) {
        throw new ParseException("'"+attributeName.getContent()+"' is already an attribute.", 
                                 attributeName.getStart(), 
                                 attributeName.getEnd(), moduleName);
      }

      recent = matchError(COLON, "':' expected.", attributeName.getEnd()).getEnd();

      final Node value = expr();

      if (isConst != null) {
        attrs.put(attributeName.getContent(), new Parameter(new Identifier(attributeName), value, false, isConst));
      }
      else{ 
        attrs.put(attributeName.getContent(), new Parameter(new Identifier(attributeName), value, false));
      }

      if (match(COMMA)) {
        continue;
      }
    }

    return new ObjectLiteral(attrs, objectKeyword.getStart(), prev().getEnd(), isSealed);
  }

  // parsing methods - END

  // Utility method - START

  private <T extends Node> T checkRuleError(Supplier<T> call, String errorMsg) {
    final Node node = call.get();
    if (node == null) {
      throw new IllegalStateException(errorMsg);
    }
    return (T) node;
  }

  private Token matchError(TokenType type, String errorMsg, Location location) throws ParseException {
    if (check(type)) {
      return consumeToken();
    }

    throw new ParseException(errorMsg, location, moduleName);
  }

  private boolean match(TokenType... types) {
    for (TokenType t : types) {
      //LOG.info(" >>> matching? "+t+" | peek: "+peek()+" | hasNext"+hasNext());
      //LOG.info(" >>> matching? "+t+" | hasNext "+hasNext());
      if (check(t)) {
        consumeToken();
        return true;
      }
    }

    return false;
  }

  private boolean match(Set<TokenType> types) {
    return match(types.toArray(new TokenType[types.size()]));
  }

  private boolean check(TokenType type) {
    if (hasNext()) {
      final Token peeked = peek();
      //LOG.info(" --- CHECK: "+peeked.getType()+" == "+type);
      return peeked.getType() == type;
    }
    return false;
  }

  private boolean hasNext() {
    return tokenStream.hasNext();
  }

  private Token peek() {
    final Token next = tokenStream.next();
    tokenStream.pushback();
    return next;
  }

  private Token prev() {
    tokenStream.pushback();
    return tokenStream.next();
  }

  private Token consumeToken() {
    if (hasNext()) {
      return tokenStream.next();
    }
    return null;
  }
  // Utility method - END
}
