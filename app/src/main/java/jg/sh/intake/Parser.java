package jg.sh.intake;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.constructs.CaptureStatement;
import jg.sh.common.FunctionSignature;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Module;
import jg.sh.intake.nodes.constructs.FuncDef;
import jg.sh.intake.nodes.constructs.Statement;
import jg.sh.intake.nodes.constructs.UseDeclaration;
import jg.sh.intake.nodes.constructs.VariableDeclr;
import jg.sh.intake.nodes.constructs.blocks.BlockExpr;
import jg.sh.intake.nodes.constructs.blocks.IfBlock;
import jg.sh.intake.nodes.constructs.blocks.WhileBlock;
import jg.sh.intake.nodes.simple.ArrayLiteral;
import jg.sh.intake.nodes.simple.AttrAccess;
import jg.sh.intake.nodes.simple.BinaryOpExpr;
import jg.sh.intake.nodes.simple.DictLiteralExpr;
import jg.sh.intake.nodes.simple.FuncCallExpr;
import jg.sh.intake.nodes.simple.Identifier;
import jg.sh.intake.nodes.simple.IndexAccessExpr;
import jg.sh.intake.nodes.simple.Keyword;
import jg.sh.intake.nodes.simple.Operator;
import jg.sh.intake.nodes.simple.ParenthesizedExpr;
import jg.sh.intake.nodes.simple.UnaryExpr;
import jg.sh.intake.nodes.simple.Operator.Op;
import jg.sh.intake.nodes.values.Bool;
import jg.sh.intake.nodes.values.Float;
import jg.sh.intake.nodes.values.Int;
import jg.sh.intake.nodes.values.NullValue;
import jg.sh.intake.nodes.values.Str;
import jg.sh.intake.nodes.simple.Parameter;
import jg.sh.intake.token.Token;
import jg.sh.intake.token.TokenType;
import jg.sh.intake.exceptions.ParseException;

public class Parser {

  private TokenizerIterator tokenStream;

  public Parser(Tokenizer tokenizer) {
    reset(tokenizer);
  }

  public Parser(List<Token> tokens) {
    reset(tokens);
  }

  public void reset(Tokenizer tokenizer) {
    this.tokenStream = tokenizer.iterator();
  }

  public void reset(List<Token> tokens) {
    this.tokenStream = new TokenizerIterator(tokens);
  }

  // parsing methods - START

  public Module parseProgram(String nameSpace, String programName) throws ParseException {
    final List<UseDeclaration> useDeclarations = new ArrayList<>();
    final Set<String> takenSymbols = new HashSet<>();
    final Map<String, FuncDef> functions = new HashMap<>();
    final Map<String, VariableDeclr> vars = new HashMap<>();
    final List<Statement> statements = new ArrayList<>();

    System.out.println("---parsing program ");

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
        final UseDeclaration useDeclaration = useDeclr(prev());
        useDeclarations.add(useDeclaration);
      }
      else if (match(TokenType.FUNC)) {
        final FuncDef funcDef = funcDef(prev(), true);
        if (takenSymbols.contains(funcDef.getBoundName())) {
          throw new IllegalStateException("The top-level symbol '" + funcDef.getBoundName() + "' is already taken");
        } else {
          takenSymbols.add(funcDef.getBoundName());
          functions.put(funcDef.getBoundName(), funcDef);
        }
      } else if (match(TokenType.DATA)) {
        final DataDef dataDef = dataDef(prev());
        if (takenSymbols.contains(dataDef.getName())) {
          throw new IllegalStateException("The top-level symbol '" + dataDef.getName() + "' is already taken");
        } else {
          takenSymbols.add(dataDef.getName());
          dataDefs.put(dataDef.getName(), dataDef);
        }
      } else if (match(TokenType.CONST, TokenType.VAR)) {
        final VariableDeclr varDeclr = varDeclr(prev());

        matchError(TokenType.SEMICOLON, "Expected ';' at " + varDeclr.end);

        if (takenSymbols.contains(varDeclr.getName().getName())) {
          throw new IllegalStateException("The top-level symbol '" + varDeclr.getName() + "' is already taken");
        } else {
          takenSymbols.add(varDeclr.getName().getName());
          vars.put(varDeclr.getName().getName(), varDeclr);
        }
      } else {
        System.out.println("---else: "+prev().getType());
        final Token unknown = peek();
        throw new IllegalStateException(
            "Unknown token '" + unknown.getContent() + "' at top level, at " + tokenEnd(unknown)+unknown.getType());
      }
    }

    //Formulate the full identifier of this module
    final String fullModuleName = nameSpace == null || nameSpace.isEmpty() ? programName : nameSpace+"."+programName;

    final Module program = new Module(fullModuleName, functions, dataDefs, vars, useDeclarations);
    return program;
  }

  private UseDeclaration useDeclr(Token useKeyword) throws ParseException {
    //Parse the module name first
    final Token moduleName = matchError(TokenType.IDENTIFIER, "Expected identifier at "+tokenEnd(useKeyword));

    final Map<Identifier, Identifier> compAliasMap = new HashMap<>();

    Token latest = null;
    Identifier moduleAlias = null;

    if (match(TokenType.IDENTIFIER)) {
      final Token aliasToken = matchError(TokenType.IDENTIFIER, "Expected identifier at "+tokenEnd(latest));
      moduleAlias = new Identifier(aliasToken.getContent(), tokenStart(aliasToken), tokenEnd(aliasToken));

      Token semiColon = matchError(TokenType.SEMICOLON, "Expected ';' to conclude use statement at "+tokenEnd(latest));
      return new UseDeclaration(tokenStart(useKeyword), 
                                tokenEnd(semiColon), 
                                moduleName.getContent(), 
                                compAliasMap,
                                moduleAlias);
    }
    else if(match(TokenType.COLON)) {
      latest = matchError(TokenType.COLON, "Expected ':' at "+tokenEnd(latest));

      while (!match(TokenType.SEMICOLON)) {
        final Token compToken = matchError(TokenType.IDENTIFIER, "Expected component name at "+tokenEnd(latest));
        if (match(TokenType.AS)) {
          final Token compAlias = matchError(TokenType.IDENTIFIER, "Expected component alias at "+tokenEnd(prev()));
          compAliasMap.put(new Identifier(compToken), new Identifier(compAlias));
        }
        else if(match(TokenType.COMMA, TokenType.SEMICOLON)) {
          compAliasMap.put(new Identifier(compToken), null);
          continue;
        }
        else {
          latest = prev();
          throw new ParseException("Unknown token '"+latest+"'in use declaration", tokenStart(latest));
        }
      }

      return new UseDeclaration(tokenStart(useKeyword), 
                              tokenEnd(prev()), 
                              moduleName.getContent(), 
                              compAliasMap,
                              moduleAlias);

    }
    else {
      throw new IllegalStateException("Unknow token '"+latest+"' in use declaration at "+tokenStart(latest));
    }
  }

  private FuncDef funcDef(Token funcKeyword, boolean topLevel) {
    System.out.println(" --> in funcDef()");

    Token funcName = null;
    if (topLevel) {
      // Top level functions require a bound name
      funcName = matchError(TokenType.IDENTIFIER, "Function name expected at " + tokenEnd(funcKeyword));
    } else if (match(TokenType.COLON)) {
      // bound names are optional for non-top-level functions
      funcName = matchError(TokenType.IDENTIFIER, "Bound function name expected at " + tokenEnd(funcKeyword));
    }

    // consume '('
    final Token leftParen = matchError(TokenType.LEFT_PAREN, "'(' expected at " + tokenEnd(funcName));
    Location latestEndLoc = tokenEnd(leftParen);

    final LinkedHashMap<String, Parameter> parameters = new LinkedHashMap<>();

    int positionalParamCount = 0;
    final HashSet<String> optionalParams = new HashSet<>();

    // check for the first parameter, and then check for the rest after
    if (match(TokenType.IDENTIFIER, TokenType.CONST)) {
      boolean allowPositionals = true;

      do {
        final Token firstToken = prev();

        boolean isConstant = false;
        Token paramName = null;
        if (firstToken.getType() == TokenType.IDENTIFIER) {
          paramName = firstToken;
        }
        else {
          isConstant = true;
          paramName = matchError(TokenType.IDENTIFIER, "Parameter name expected at " + tokenEnd(firstToken));
        }

        if (parameters.containsKey(paramName)) {
          throw new ParseException("Repeated parameter '"+paramName.getContent()+"'", tokenStart(paramName));
        }

        if (match(TokenType.ASSIGNMENT)) {
          allowPositionals = false;

          final Node initialVal = checkRuleError(this::expr, "Expected an initial value at "+tokenEnd(prev()));
          final Parameter parameter = new Parameter(paramName.getContent(), initialVal, isConstant, tokenStart(firstToken), initialVal.end);
          parameters.put(parameter.getName(), parameter);
          optionalParams.add(parameter.getName());
        }
        else {
          if (!allowPositionals) {
            throw new ParseException("All positional parameters must be placed before positional parameters.", tokenEnd(prev()));
          }

          final Parameter parameter = new Parameter(paramName.getContent(), null, isConstant, tokenStart(firstToken), tokenEnd(firstToken));
          parameters.put(parameter.getName(), parameter);
          positionalParamCount++;
        }
      } while (match(TokenType.COMMA));
    }

    // consume ')'
    final Token rightParen = matchError(TokenType.RIGHT_PAREN, "')' expected at " + latestEndLoc);

    // Consume '{'
    final Token bodyLeftCurl = matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(rightParen));

    // Consume function block
    final BlockExpr funcBody = blockExpr(bodyLeftCurl);

    final FunctionSignature signature = new FunctionSignature(positionalParamCount, optionalParams);

    if (funcBody.getStatements().size() > 0 && funcBody.getStatements().get(0) instanceof CaptureStatement) {
      final CaptureStatement captureStatement = (CaptureStatement) funcBody.getStatements().get(0);
      return new FuncDef(tokenStart(funcKeyword), 
                         funcBody.end, 
                         funcName.getContent(), 
                         signature, 
                         captureStatement, 
                         parameters, 
                         funcBody);
    }

    return new FuncDef(tokenStart(funcKeyword), 
                       funcBody.end, 
                       funcName.getContent(), 
                       signature, 
                       null,
                        parameters, 
                        funcBody);
  }

  /*
  private InterfaceDef interfaceDef(Token interfaceKeyword) {
    final Token name = matchError(TokenType.IDENTIFIER,
        "Interface type name expected at " + tokenEnd(interfaceKeyword));
    matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(name));

    final LinkedHashMap<Identifier, FuncTypeAnnotation> funcs = new LinkedHashMap<>();

    while (match(TokenType.IDENTIFIER)) {
      final Token nameToken = prev();
      final Identifier attrName = new Identifier(nameToken.getContent(), tokenStart(nameToken), tokenEnd(nameToken));

      final Token colonToken = matchError(TokenType.COLON, "':' expected at " + attrName.end);

      TypeAnnotation attrType = checkRuleError(() -> type(),
          "Attribute type declaration expected at " + tokenEnd(colonToken));
      if (!(attrType instanceof FuncTypeAnnotation)) {
        throw new IllegalStateException("Only function types are expected in an interface, at " + tokenEnd(colonToken));
      }
      if (attrType.getName().equals(name.getContent())) {
        // If this attribute's type is the data def itself, replace the type annotation
        // to be "self" instead
        attrType = new TypeAnnotation(TokenType.SELF.name().toLowerCase(),
            attrType.isNullable(),
            attrType.start,
            attrType.end);
      }

      matchError(TokenType.SEMICOLON, "';' expected at " + attrType.end);

      if (funcs.containsKey(attrName)) {
        throw new IllegalStateException("'" + attrName.getName() + "' has already been used, at " + attrName.start);
      } else {
        funcs.put(attrName, (FuncTypeAnnotation) attrType);
      }
    }

    final Token rightCurl = matchError(TokenType.RIGHT_CURL, "'}' expected at " + tokenEnd(name));
    return new InterfaceDef(name.getContent(), funcs, tokenStart(interfaceKeyword), tokenEnd(rightCurl));
  }
  */

  private FuncDef dataDef(Token dataKeyword) {
    /*
     * A data definition is a syntactic sugar for a way for structuring
     * the construction of an object.
     * 
     * Internally, a data definition is a function.
     */

    final Token name = matchError(TokenType.IDENTIFIER, "Data type name expected at " + tokenEnd(dataKeyword));

    matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(name));

    final LinkedHashMap<Identifier, TypeAnnotation> attrs = new LinkedHashMap<>();

    while (match(TokenType.IDENTIFIER)) {
      final Token nameToken = prev();
      final Identifier attrName = new Identifier(nameToken.getContent(), tokenStart(nameToken), tokenEnd(nameToken));

      final Token colonToken = matchError(TokenType.COLON, "':' expected at " + attrName.end);

      TypeAnnotation attrType = checkRuleError(() -> type(),
          "Attribute type declaration expected at " + tokenEnd(colonToken));
      if (attrType.getName().equals(name.getContent())) {
        // If this attribute's type is the data def itself, replace the type annotation
        // to be "self" instead
        attrType = new TypeAnnotation(TokenType.SELF.name().toLowerCase(),
            attrType.isNullable(),
            attrType.start,
            attrType.end);
      }

      matchError(TokenType.SEMICOLON, "';' expected at " + attrType.end);

      if (attrs.containsKey(attrName)) {
        throw new IllegalStateException("'" + attrName.getName() + "' has already been used, at " + attrName.start);
      } else {
        attrs.put(attrName, attrType);
      }
    }

    final Token rightCurl = matchError(TokenType.RIGHT_CURL, "'}' expected at " + tokenEnd(name));
    return new DataDef(name.getContent(), attrs, tokenStart(dataKeyword), tokenEnd(rightCurl));
  }

  private VariableDeclr varDeclr(Token signifier) {
    final Token varName = matchError(TokenType.IDENTIFIER, "Expected an identifier");
    final Identifier varIden = new Identifier(varName.getContent(), tokenStart(varName), tokenEnd(varName));

    final TypeAnnotation declaredType = match(TokenType.COLON)
        ? checkRuleError(() -> type(), "Expected type declaration at " + tokenEnd(prev()))
        : null;

    final Token assgnOp = matchError(TokenType.ASSIGNMENT, "Expected ':='");
    final Node initialValue = checkRuleError(() -> expr(), "Expected assignment value at " + tokenEnd(assgnOp));
    return new VariableDeclr(varIden, signifier.getType() == TokenType.FINAL, declaredType, initialValue, varIden.start,
        initialValue.end);
  }

  private Statement statement() {
    final Node expr = expr();

    System.out.println(" --> in statement()");

    if (expr == null) {
      return null;
    } else if (!(expr instanceof BlockExpr || expr instanceof FuncDef)) {
      matchError(TokenType.SEMICOLON, "Semicolon expected at " + expr.end);
    }

    System.out.println(" ** non null expr for stmt: " + expr);

    return new Statement(expr);
  }

  public Node expr() {
    Node initial = null;

    System.out.println(" --> in expr() ");

    if (match(TokenType.LEFT_CURL)) {
      final Token matched = prev();
      System.out.println("    ===> TO blockExpr() for " + matched);
      initial = blockExpr(matched);
    } else if (match(TokenType.IF)) {
      initial = ifBlock(prev());
    } else if (match(TokenType.WHILE)) {
      initial = whileBlock(prev());
    } else if (match(TokenType.LEFT_SQ_BR)) {
      initial = arrayLiteral(prev());
    } else if (match(TokenType.LEFT_CURL)) {
      initial = blockExpr(prev());
    } else if (match(TokenType.DICT)) {
      initial = dictLiteral(prev());
    } else if (match(TokenType.LEFT_PAREN)) {
      initial = parenExpr(prev());
    } else if (match(TokenType.FUNC)) {
      initial = funcDef(prev(), false);
    } else if (match(TokenType.LET, TokenType.FINAL)) {
      initial = varDeclr(prev());
    } else if (match(TokenType.BREAK, TokenType.CONTINUE, TokenType.RETURN)) {
      final Token keyword = prev();
      return new Keyword(TokenType.valueOf(keyword.getContent().toUpperCase()), tokenStart(keyword), tokenEnd(keyword));
    } else if (match(TokenType.MINUS, TokenType.BANG)) {
      final Token unaryOp = prev();
      final Operator operator = new Operator(Op.valueOf(unaryOp.getType().name().toUpperCase()),
          tokenStart(unaryOp),
          tokenEnd(unaryOp));
      final Node unaryTarget = checkRuleError(() -> expr(), "Unary target expression expected at " + tokenEnd(unaryOp));
      initial = new UnaryExpr(operator, unaryTarget);
    } else {
      initial = primary();
      System.out.println("  --> back from primary() " + (initial == null) + " " + initial);
      if (initial == null) {
        return null;
      }

      if (match(TokenType.binOps)) {
        initial = binOpExpr(initial);
      }
    }

    loop: while (match(TokenType.LEFT_PAREN, TokenType.LEFT_SQ_BR, TokenType.DOT, TokenType.AS, TokenType.IS_A)) {
      final Token matched = prev();

      switch (matched.getType()) {
        case LEFT_PAREN:
          initial = funcCall(initial, matched);
          break;
        case LEFT_SQ_BR:
          initial = indexAccess(initial, matched);
          break;
        case DOT:
          initial = attrAccess(initial, matched);
          break;
        case IS_A:
          break;
        case AS:
          initial = new CastExpr(initial,
              checkRuleError(() -> type(), "Type annotation expected at cast expression, at " + tokenEnd(matched)),
              initial.start,
              tokenEnd(matched));
          break;
        default:
          break loop;
      }
    }

    return initial;
  }

  private InstantiationExpr instantiation(String target, Token leftCurly) {
    final HashMap<Identifier, Node> args = new HashMap<>();

    Location recentLoc = tokenEnd(leftCurly);

    while (!match(TokenType.RIGHT_CURL)) {
      final Token attrName = matchError(TokenType.IDENTIFIER, "Attribute name expected at " + recentLoc);
      final Identifier attrIden = new Identifier(attrName.getContent(), tokenStart(attrName), tokenEnd(attrName));

      matchError(TokenType.ASSIGNMENT, "':=' (attribute assignment) expected at " + tokenEnd(attrName));
      final Node attrValue = checkRuleError(() -> expr(), "Attributes need to be assigned, at " + tokenEnd(attrName));

      recentLoc = attrValue.end;

      if (args.containsKey(attrIden)) {
        throw new IllegalStateException(
            "Attribute '" + attrIden.getName() + "' has already been assigned, at " + attrIden.end);
      } else {
        args.put(attrIden, attrValue);
      }

      if (!match(TokenType.COMMA, TokenType.RIGHT_CURL)) {
        throw new IllegalStateException("',' or '}' expected at function call, at " + attrValue.end);
      } else if (prev().getType() == TokenType.RIGHT_CURL) {
        break;
      }
    }

    return new InstantiationExpr(target, args, tokenStart(leftCurly), tokenEnd(prev()));
  }

  private IndexAccessExpr indexAccess(Node targetExpr, Token leftSqBrace) {
    final Node indexValue = expr();
    final Token rightSqBrace = matchError(TokenType.RIGHT_SQ_BR, "']' expected at index access, at " + indexValue.end);
    return new IndexAccessExpr(targetExpr, indexValue, targetExpr.start, tokenEnd(rightSqBrace));
  }

  private AttrAccess attrAccess(Node targetExpr, Token dotToken) {
    final Token attrNameToken = matchError(TokenType.IDENTIFIER, "Attribute name expected at " + tokenEnd(dotToken));
    final Identifier attrIden = new Identifier(attrNameToken.getContent(), tokenStart(attrNameToken),
        tokenEnd(attrNameToken));
    return new AttrAccess(targetExpr, attrIden, tokenStart(attrNameToken), attrIden.end);
  }

  private FuncCallExpr funcCall(Node targetExpr, Token leftParen) {
    final ArrayList<Node> args = new ArrayList<>();

    Location recentLoc = tokenEnd(leftParen);

    while (!match(TokenType.RIGHT_PAREN)) {
      final Node arrElement = checkRuleError(() -> expr(), "Function argument expected at " + recentLoc);
      recentLoc = arrElement.end;

      args.add(arrElement);
      if (!match(TokenType.COMMA, TokenType.RIGHT_PAREN)) {
        throw new IllegalStateException("',' or ')' expected at function call, at " + arrElement.end);
      } else if (prev().getType() == TokenType.RIGHT_PAREN) {
        break;
      }
    }

    final Token rightCurl = prev();

    return new FuncCallExpr(targetExpr, args.toArray(new Node[args.size()]), targetExpr.start, tokenEnd(rightCurl));
  }

  private ParenthesizedExpr parenExpr(Token leftParen) {
    final Node innerExpr = expr();
    final Token rParen = matchError(TokenType.RIGHT_PAREN, "')' expected at " + innerExpr.end);
    return new ParenthesizedExpr(innerExpr, tokenStart(leftParen), tokenEnd(rParen));
  }

  private ArrayLiteral arrayLiteral(Token leftSqToken) {
    final ArrayList<Node> arrayContent = new ArrayList<>();

    System.out.println(" --> in arrayLiteral()");

    Location recentLoc = tokenEnd(leftSqToken);

    while (!match(TokenType.RIGHT_SQ_BR)) {
      final Node arrElement = checkRuleError(() -> expr(), "Array element expected at " + recentLoc);
      recentLoc = arrElement.end;

      arrayContent.add(arrElement);
      if (!match(TokenType.COMMA, TokenType.RIGHT_SQ_BR)) {
        throw new IllegalStateException("',' or ']' expected at array literal, at " + arrElement.end);
      } else if (prev().getType() == TokenType.RIGHT_SQ_BR) {
        break;
      }
    }

    final Token rightCurl = prev();

    return new ArrayLiteral(arrayContent, tokenStart(leftSqToken), tokenEnd(rightCurl));
  }

  private DictLiteralExpr dictLiteral(Token dictKeyword) {
    final HashMap<Identifier, Node> dictAttrs = new HashMap<>();

    Location recentLoc = tokenEnd(matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(dictKeyword)));
    while (match(TokenType.LET)) {
      final VariableDeclr dictAttr = varDeclr(prev());

      if (dictAttr.getInitialValue() == null) {
        throw new IllegalStateException("The attribute '" + dictAttr.getName().getName()
            + "' needs to be assigned a value, at " + dictAttr.getName().end);
      }

      if (dictAttrs.containsKey(dictAttr.getName())) {
        throw new IllegalStateException("'" + dictAttr.getName().getName()
            + "' is already an attribute of this dictionary, at " + dictAttr.getName().end);
      } else {
        dictAttrs.put(dictAttr.getName(), dictAttr.getInitialValue());
      }

      matchError(TokenType.SEMICOLON, "Semicolon expected at " + dictAttr.end);

      recentLoc = dictAttr.end;
    }

    final Token rightCurl = matchError(TokenType.RIGHT_CURL, "'}' expected at " + recentLoc);

    return new DictLiteralExpr(dictAttrs, tokenStart(dictKeyword), tokenEnd(rightCurl));
  }

  private IfBlock ifBlock(Token ifKeyword) {
    final Token leftParen = matchError(TokenType.LEFT_PAREN, "'(' expected at " + tokenEnd(ifKeyword));
    final Node condition = checkRuleError(() -> expr(), "Expression expected for if block, at " + tokenEnd(leftParen));
    final Token rightParen = matchError(TokenType.RIGHT_PAREN, "')' expected at " + condition.end);

    final Token leftCurly = matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(rightParen));
    final BlockExpr ifBlock = blockExpr(leftCurly);

    final ArrayList<IfBlock> alternates = new ArrayList<>();

    Location recentLocation = ifBlock.end;
    while (match(TokenType.ELIF, TokenType.ELSE)) {
      ifKeyword = prev();
      if (ifKeyword.getType() == TokenType.ELIF) {
        // This is an elif block
        final Token altLeftParen = matchError(TokenType.LEFT_PAREN, "'(' expected at " + tokenEnd(ifKeyword));
        final Node altCond = checkRuleError(() -> expr(),
            "Expression expected for elif block, at " + tokenEnd(altLeftParen));
        final Token altRightParen = matchError(TokenType.RIGHT_PAREN, "')' expected at " + altCond.end);
        final Token altLeftCurly = matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(altRightParen));
        final BlockExpr altElifBlock = blockExpr(altLeftCurly);

        recentLocation = altElifBlock.end;

        alternates.add(new IfBlock(ifKeyword.getType(), altCond, altElifBlock.getStatements(), tokenStart(ifKeyword),
            altElifBlock.end));
      } else {
        // This is an else block
        final Token altLeftCurly = matchError(TokenType.LEFT_CURL, "'{' expected at " + recentLocation);
        final BlockExpr altElseBlock = blockExpr(altLeftCurly);
        alternates.add(new IfBlock(ifKeyword.getType(), null, altElseBlock.getStatements(), tokenStart(ifKeyword),
            altElseBlock.end));
        break;
      }
    }

    return new IfBlock(ifKeyword.getType(), condition, ifBlock.getStatements(), alternates, tokenStart(ifKeyword),
        ifBlock.end);
  }

  private WhileBlock whileBlock(Token whileKeyword) {
    final Token leftParen = matchError(TokenType.LEFT_PAREN, "'(' expected at " + tokenEnd(whileKeyword));
    final Node condition = checkRuleError(() -> expr(),
        "Expression expected for while block, at " + tokenEnd(leftParen));
    final Token rightParen = matchError(TokenType.RIGHT_PAREN, "')' expected at " + condition.end);

    final Token leftCurly = matchError(TokenType.LEFT_CURL, "'{' expected at " + tokenEnd(rightParen));
    final BlockExpr whileBlock = blockExpr(leftCurly);
    return new WhileBlock(condition, whileBlock.getStatements(), tokenStart(whileKeyword), whileBlock.end);
  }

  private BlockExpr blockExpr(Token leftCurlToken) {
    System.out.println(" --> in blockExpr()");

    final ArrayList<Statement> statements = new ArrayList<>();

    Statement currentStatement = null;
    while ((currentStatement = statement()) != null) {
      statements.add(currentStatement);
    }

    final Token rightCurlToken = matchError(TokenType.RIGHT_CURL,
        "'}' expected at " +
            (currentStatement == null ? tokenEnd(leftCurlToken) : currentStatement.end));

    return new BlockExpr(statements, tokenStart(leftCurlToken), tokenEnd(rightCurlToken));
  }

  private Node binOpExpr(Node leftOperand) {
    do {
      System.out.println("--- bin expr loop");
      final Token matched = prev();
      final Op op = Op.valueOf(matched.getType().name().toUpperCase());

      if (Op.mutatesLeft(op) && !leftOperand.isLValue()) {
        throw new IllegalStateException(
            "Left hand side of an assignment operator must be assignable, at " + leftOperand.end);
      }

      final Location[] opLocs = tokenLocation(matched);
      final Node rightOperand = checkRuleError(() -> expr(), "Right operand expected at " + opLocs[1]);
      leftOperand = new BinaryOpExpr(new Operator(op, opLocs[0], opLocs[1]), leftOperand, rightOperand);
    } while (match(TokenType.binOps));

    System.out.println("--end of binopExpr");

    return leftOperand;
  }

  private TypeAnnotation type() {
    if (match(TokenType.INT,
        TokenType.FLOAT,
        TokenType.BOOL,
        TokenType.VOID,
        TokenType.STR)) {
      // Primitive types nad void that ARE NOT nullable ever
      final Token typeName = prev();
      final Location[] typeLocs = tokenLocation(typeName);

      if (match(TokenType.QUESTION)) {
        final Token nullable = prev();

        if (typeName.getType() == TokenType.ANY) {
          throw new IllegalArgumentException(
              "The 'any' type is implicitly nullable due as it's a unifying type, loc: " + tokenLocation(nullable)[0]);
        }
        throw new IllegalArgumentException(
            "int, float and booleans cannot be nullable, loc: " + tokenLocation(nullable)[0]);
      }

      return new TypeAnnotation(typeName.getContent(), false, typeLocs[0], typeLocs[1]);
    } else if (match(TokenType.ANY,
        TokenType.IDENTIFIER)) {
      // Primitive types that ARE nullable
      final Token typeName = prev();
      final Location[] typeLocs = tokenLocation(typeName);
      return new TypeAnnotation(typeName.getContent(), match(TokenType.QUESTION), typeLocs[0], typeLocs[1]);
    } else if (match(TokenType.SELF)) {
      final Token selfToken = prev();
      final TypeAnnotation selfType = new TypeAnnotation(selfToken.getContent(),
          match(TokenType.QUESTION),
          tokenStart(selfToken),
          tokenEnd(selfToken));
      return selfType;
    } else if (match(TokenType.LEFT_SQ_BR)) {
      // Array type: '[' type ']'
      final Token leftSquareBrace = prev();
      final Location[] leftSquareLocs = tokenLocation(leftSquareBrace);

      final TypeAnnotation innerType = type();

      final Token rightSquareBrace = matchError(TokenType.RIGHT_SQ_BR, "']' expected at: " + innerType.end);
      final Location[] rightSquareLocs = tokenLocation(rightSquareBrace);

      return new ArrayTypeAnnotation(innerType, match(TokenType.QUESTION), leftSquareLocs[0], rightSquareLocs[1]);
    } else if (match(TokenType.LEFT_CURL)) {
      // Dictionary type: '{' [(name ':' type) (',' (name ':' type))* ] '}'
      final Token leftCurlBrace = prev();

      final HashMap<String, TypeAnnotation> attrTypes = new HashMap<>();
      TypeAnnotation lastType = null;

      while (match(TokenType.IDENTIFIER)) {
        final Token attrName = prev();
        final Token colon = matchError(TokenType.COLON, "':' expected at " + tokenStart(attrName));

        lastType = checkRuleError(() -> type(), "Type expected at " + tokenStart(colon));
        matchError(TokenType.SEMICOLON, "';' expected at " + lastType.end);

        if (attrTypes.containsKey(attrName.getContent())) {
          throw new IllegalStateException(
              "'" + attrName.getContent() + "' has already been declared, at " + tokenStart(attrName));
        } else {
          attrTypes.put(attrName.getContent(), lastType);
        }
      }

      final Token rightCurlBrace = matchError(TokenType.RIGHT_CURL,
          "'}' expected at: " + (lastType == null ? tokenEnd(leftCurlBrace) : lastType.end));

      return new DictionaryTypeAnnotation(attrTypes, match(TokenType.QUESTION), tokenStart(leftCurlBrace),
          tokenEnd(rightCurlBrace));
    } else if (match(TokenType.LEFT_PAREN)) {
      // Function type: '(' [type (',' type)*] ')' '->' type
      final Token leftParen = prev();

      final ArrayList<TypeAnnotation> paramTypes = new ArrayList<>();
      TypeAnnotation lastType = null;

      while ((lastType = type()) != null) {
        paramTypes.add(lastType);

        if (!match(TokenType.COMMA)) {
          break;
        }
      }

      final Token rightParen = matchError(TokenType.RIGHT_PAREN,
          "')' expected at: " + (lastType == null ? tokenEnd(leftParen) : lastType.end));

      final Token arrowToken = matchError(TokenType.ARROW, "'->' expected at " + tokenEnd(rightParen));
      final TypeAnnotation returnType = checkRuleError(() -> type(), "Type expected at " + tokenEnd(arrowToken));

      return new FuncTypeAnnotation(paramTypes.toArray(new TypeAnnotation[paramTypes.size()]),
          returnType,
          match(TokenType.QUESTION),
          tokenStart(leftParen),
          returnType.end);
    }

    return null;
  }

  /**
   * Literals (number, string, booleans), parenthesized, and nulls
   * 
   * @return
   */
  private Node primary() {
    System.out.println(" --> in primary()");
    if (match(TokenType.TRUE)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new Bool(true, locs[0], locs[1]);
    } else if (match(TokenType.FALSE)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new Bool(false, locs[0], locs[1]);
    } else if (match(TokenType.INTEGER)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new Int(Integer.parseInt(matched.getContent()), locs[0], locs[1]);
    } else if (match(TokenType.DECIMAL)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new Float(Double.parseDouble(matched.getContent()), locs[0], locs[1]);
    } else if (match(TokenType.STRING)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new Str(matched.getContent(), locs[0], locs[1]);
    } else if (match(TokenType.IDENTIFIER)) {
      final Token nameToken = prev();

      if (match(TokenType.LEFT_CURL)) {
        return instantiation(nameToken.getContent(), prev());
      } else {
        return new Identifier(nameToken.getContent(), tokenStart(nameToken), tokenEnd(nameToken));
      }
    } else if (match(TokenType.NULL)) {
      final Token matched = prev();
      final Location[] locs = tokenLocation(matched);
      return new NullValue(locs[0], locs[1]);
    } else if (match(TokenType.LEFT_PAREN)) {
      final Token matched = prev();
      final Location[] leftParenLocs = tokenLocation(matched);
      final Node expr = expr();
      final Token rParen = matchError(TokenType.RIGHT_PAREN, "Expected '('");
      final Location[] rightParenLocs = tokenLocation(rParen);
      return new ParenthesizedExpr(expr, leftParenLocs[0], rightParenLocs[1]);
    }

    return null;
  }

  // parsing methods - END

  // Utility method - START

  private Location[] tokenLocation(Token token) {
    final Location[] locations = new Location[2];
    locations[0] = new Location(token.getLineNumber(), token.getStartCol());
    locations[1] = new Location(token.getLineNumber(), token.getEndCol());
    return locations;
  }

  private Location tokenStart(Token token) {
    return new Location(token.getLineNumber(), token.getStartCol());
  }

  private Location tokenEnd(Token token) {
    return new Location(token.getLineNumber(), token.getEndCol());
  }

  private <T extends Node> T checkRuleError(Supplier<T> call, String errorMsg) {
    final Node node = call.get();
    if (node == null) {
      throw new IllegalStateException(errorMsg);
    }
    return (T) node;
  }

  private Token matchError(TokenType type, String errorMsg) {
    if (check(type)) {
      return consumeToken();
    }

    throw new IllegalStateException(errorMsg);
  }

  private boolean match(TokenType... types) {
    for (TokenType t : types) {
      //System.out.println(" >>> matching? "+t+" | peek: "+peek()+" | hasNext"+hasNext());
      System.out.println(" >>> matching? "+t+" | hasNext "+hasNext());
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
      System.out.println(" --- CHECK: "+peeked.getType()+" == "+type);
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
