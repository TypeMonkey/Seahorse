package jg.sh.parsing.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.VarDeclr;
import jg.sh.parsing.nodes.statements.blocks.Block;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Str;
import jg.sh.parsing.nodes.values.Value;
import jg.sh.parsing.token.TokenType;

public final class ParseTestUtils {
  
  public static void assertSignature(int positionalCount, 
                                     Set<String> optionals, 
                                     boolean hasVariedParam, 
                                     boolean hasVariedKeywordParam,
                                     FunctionSignature signature) {
    assertEquals(positionalCount, signature.getPositionalParamCount());
    assertTrue(signature.getKeywordParams().containsAll(optionals), "Expected optional parameters missing in signature.");
    assertTrue(optionals.containsAll(signature.getKeywordParams()), "Declared parameters missing in expected.");
    assertEquals(hasVariedParam, signature.hasVariableParams());
    assertEquals(hasVariedKeywordParam, signature.hasVarKeywordParams());
  }

  public static void assertSignature(FunctionSignature expected, 
                                     FunctionSignature signature) {
    assertSignature(expected.getPositionalParamCount(), 
                    expected.getKeywordParams(), 
                    expected.hasVariableParams(), 
                    expected.hasVarKeywordParams(),
                    signature);
  }

  public static FuncDef assertHasFunc(String name, 
                                      int positionalCount, 
                                      Set<String> optionals, 
                                      boolean hasVariedParam, 
                                      Collection<Statement> statements) {
    for (Statement statement : statements) {
      if (statement.getExpr() instanceof FuncDef) {
        final FuncDef funcDef = (FuncDef) statement.getExpr();
        final FunctionSignature signature = funcDef.getSignature();
        System.out.println("=> "+signature);
        if (funcDef.hasBoundName() && 
            funcDef.getBoundName().getIdentifier().equals(name) &&
            signature.getPositionalParamCount() == positionalCount && 
            signature.hasVariableParams() == hasVariedParam &&
            signature.getKeywordParams().containsAll(optionals) &&
            optionals.containsAll(signature.getKeywordParams())) {
          return funcDef;
        }
      }
    }

    fail("No such function found!");
    return null;
  }

  public static VarDeclr assertHasVar(String name, 
                                  boolean isConst, 
                                  boolean isExported, 
                                  boolean hasInitValue, 
                                  Collection<Statement> statements) {
    for (Statement statement : statements) {
      if (statement instanceof VarDeclr) {
        final VarDeclr varDeclr = (VarDeclr) statement;
        System.out.println(" ===> var: "+varDeclr.getName()+" | "+varDeclr.isConst());
        if (varDeclr.getName().getIdentifier().equals(name) &&
            varDeclr.isConst() == isConst &&
            varDeclr.hasInitialValue() == hasInitValue) {
          return varDeclr;
        }
      }
    }

    fail("No such variable found!");
    return null;
  }

  public static void assertIden(String expected, Identifier identifier) {
    assertEquals(expected, identifier.getIdentifier());
  }

  public static <T> T assertAndCast(Class<T> c, Object value) {
    assertInstanceOf(c, value, "Expected class: "+c+", target is "+value.toString());
    return (T) value;
  }

  public static void assertInt(long expected, Node node) {
    assertEquals(expected, assertAndCast(Int.class, node).getValue());
  }

  public static void assertStr(String expected, Node node) {
    assertEquals(expected, assertAndCast(Str.class, node).getValue());
  }

  public static void assertFloat(double expected, Node node) {
    assertEquals(expected, assertAndCast(FloatingPoint.class, node).getValue());
  }

  public static void assertBool(boolean expected, Node node) {
    assertEquals(expected, assertAndCast(Bool.class, node).getValue());
  }

  public static void assertName(String expected, Node node) {
    assertEquals(expected, assertAndCast(Identifier.class, node).getIdentifier());
  }

  public static void assertIndex(String name, long index, IndexAccess access) {
    assertName(name, access.getTarget());
    assertInt(index, access.getIndex());
  }

  public static <T> T assertNest(Class<T> c, int expectedLevel, Parenthesized parenthesized) {
    Node ret = parenthesized;
    while (expectedLevel > 0) {
      if (ret instanceof Parenthesized) {
        ret = ((Parenthesized) ret).getInner();
      }
      else {
        fail(expectedLevel+" layers left!");
      }
      expectedLevel--;
    }

    if (ret instanceof Parenthesized) {
      fail("Latest layer is still parenthesized!");
    }

    return assertAndCast(c, ret);
  }

  public static void assertBinEquals(BinaryOpExpr expected, Node actual) {
    final BinaryOpExpr actualBin = assertAndCast(BinaryOpExpr.class, actual);
    assertNodeEquals(expected.getLeft(), actualBin.getLeft());
    assertEquals(expected.getOperator(), actualBin.getOperator());
    assertNodeEquals(expected.getRight(), actualBin.getRight());
  }

  public static void assertNodeEquals(Node expected, Node actual) {
    if (expected == actual) {
      return;
    }
    if (expected == null && actual != null) {
      fail("Expected null but actual is "+actual.repr());
    }
    else if (expected != null && actual == null) {
      fail("Expected "+expected.repr()+" but actual is null");
    }
    if (expected instanceof Value) {
      final Value<?> actualValue = assertAndCast(Value.class, actual);
      assertEquals(expected, actualValue, "Expecting value "+expected.repr()+" but got "+actual.repr());
    }
    else if(expected instanceof Identifier) {
      final Identifier actualIden = assertAndCast(Identifier.class, actual);
      assertEquals(expected, actualIden, "Expecting identifier "+expected.repr()+" but got "+actual.repr());
    }
    else if(expected instanceof UnaryExpr) {
      final UnaryExpr expectedUnary = (UnaryExpr) expected;
      final UnaryExpr actualUnary = assertAndCast(UnaryExpr.class, actual);
      assertEquals(expectedUnary.getOperator().getOp(), actualUnary.getOperator().getOp());
      assertNodeEquals(expectedUnary.getTarget(), actualUnary.getTarget());
    }
    else if(expected instanceof Parenthesized) {
      final Parenthesized parenthesized = (Parenthesized) expected;
      final Parenthesized actualParen = assertAndCast(Parenthesized.class, actual);
      assertNodeEquals(parenthesized.getInner(), actualParen.getInner());
    }
    else if(expected instanceof BinaryOpExpr) {
      final BinaryOpExpr expectedBinaryOpExpr = assertAndCast(BinaryOpExpr.class, expected);
      System.out.println(" ===> expected: "+expected+" ("+expected.getClass()+") || target: "+actual+" ("+actual.getClass()+")");
      assertBinEquals(expectedBinaryOpExpr, actual);
    }
    else if(expected instanceof IndexAccess) {
      final IndexAccess expectedIndex = (IndexAccess) expected;
      final IndexAccess actualIndex = assertAndCast(IndexAccess.class, actual);
      assertNodeEquals(expectedIndex.getTarget(), actualIndex.getTarget());
      assertNodeEquals(expectedIndex.getIndex(), actualIndex.getIndex());
    }
    else if(expected instanceof AttrAccess) {
      final AttrAccess expectedAcc = (AttrAccess) expected;
      final AttrAccess actualAcc = assertAndCast(AttrAccess.class, actual);
      assertNodeEquals(expectedAcc.getTarget(), actualAcc.getTarget());
      assertNodeEquals(expectedAcc.getAttrName(), actualAcc.getAttrName());
    }
    else if (expected instanceof FuncCall) {
      final FuncCall expectedCall = (FuncCall) expected;
      final FuncCall actualCall = assertAndCast(FuncCall.class, actual);

      assertNodeEquals(expectedCall.getTarget(), actualCall.getTarget());

      //check arguments
      assertEquals(expectedCall.getArguments().length, actualCall.getArguments().length);

      for (int i = 0; i < expectedCall.getArguments().length; i++) {
        final Argument expectedArg = expectedCall.getArguments()[i];
        final Argument actualArg = actualCall.getArguments()[i];

        assertNodeEquals(expectedArg.getParamName(), actualArg.getParamName());
        assertNodeEquals(expectedArg.getArgument(), actualArg.getArgument());
      }
    }
    else if(expected instanceof FuncDef) {
      final FuncDef expectedFunc = (FuncDef) expected;
      final FuncDef actualFunc = assertAndCast(FuncDef.class, actual);
      
      //check bound name
      assertNodeEquals(expectedFunc.getBoundName(), actualFunc.getBoundName());

      //Check signature
      assertSignature(expectedFunc.getSignature(), actualFunc.getSignature());

      //Check captures
      assertTrue(expectedFunc.getCaptures().containsAll(actualFunc.getCaptures()) && 
                 actualFunc.getCaptures().containsAll(expectedFunc.getCaptures()));

      //Check parameters
      assertTrue(expectedFunc.getParameters().keySet().containsAll(actualFunc.getParameters().keySet()) && 
                 actualFunc.getParameters().keySet().containsAll(expectedFunc.getParameters().keySet()));

      for (Entry<String, Parameter> expectedParam : expectedFunc.getParameters().entrySet()) {
        final Parameter actualParam = actualFunc.getParameters().get(expectedParam.getKey());
        assertNotNull(actualParam);
        assertEquals(expectedParam.getValue().hasValue(), actualParam.hasValue());
        assertNodeEquals(expectedParam.getValue().getInitValue(), actualParam.getInitValue());
      }

      //Check if export
      assertEquals(expectedFunc.toExport(), actualFunc.toExport());

      //Check body
      assertStmt(expectedFunc.getBody(), actualFunc.getBody());
    }
    else {
      fail("No matching condition. Expected: "+
           (expected == null ? "null" : expected.getClass())+
           " . Actual "+
           (actual == null ? "null" : actual.getClass()));
    }
  } 

  public static void assertStmt(Statement expected, Statement actual) {
    if (expected instanceof ReturnStatement) {
      final ReturnStatement expectedReturn = (ReturnStatement) expected;
      final ReturnStatement actualReturn = assertAndCast(ReturnStatement.class, actual);
      assertNodeEquals(expectedReturn.getValue(), actualReturn.getValue());
    }
    else if(expected instanceof Block) {
      final Block expectedBlock = (Block) expected;
      final Block actualBlock = assertAndCast(Block.class, actual);
      
      assertEquals(expectedBlock.getStatements().size(), actualBlock.getStatements().size());

      for (int i = 0; i < expectedBlock.getStatements().size(); i++) {
        final Statement expectedStmt = expectedBlock.get(i);
        final Statement actualStmt = actualBlock.get(i);

        assertStmt(expectedStmt, actualStmt);
      }
    }
    else {
      fail("No matching condition. Expected: "+
           (expected == null ? "null" : expected.getClass())+
           " . Actual "+
           (actual == null ? "null" : actual.getClass()));
    }
  } 

  //Wrapper methods
  public static FuncCall call(Node target) {
    return new FuncCall(target, Location.DUMMY);
  }

  public static ReturnStatement ret(Node value) {
    return value == null ? 
            new ReturnStatement(keyword(TokenType.RETURN), Location.DUMMY) : 
            new ReturnStatement(keyword(TokenType.RETURN), value, Location.DUMMY);
  }

  public static FunctionSignature signature(int pos, boolean varArgs, boolean varArgsKeys, String ... optionals) {
    return new FunctionSignature(pos, new HashSet<>(Arrays.asList(optionals)), varArgs, varArgsKeys);
  }

  public static FuncDef simpleFunc(String name, Node returnValue) {
    return new FuncDef(name(name), 
                       FunctionSignature.NO_ARG, 
                       Collections.emptySet(), 
                       new LinkedHashMap<>(), 
                       false, 
                       new Block(
                        Arrays.asList(new ReturnStatement(keyword(TokenType.RETURN), returnValue, null)), 
                        Location.DUMMY, 
                        Location.DUMMY), 
                      Location.DUMMY, 
                      Location.DUMMY);
  }

  public static Keyword keyword(TokenType tokenType) {
    return new Keyword(tokenType, Location.DUMMY, Location.DUMMY);
  }


  public static BinaryOpExpr bin(Node left, Op op, Node right) {
    return new BinaryOpExpr(left, right, new Operator(op,  Location.DUMMY, Location.DUMMY));
  }

  public static UnaryExpr unary(Op op, Node target) {
    return new UnaryExpr(new Operator(op, Location.DUMMY, Location.DUMMY) , target);
  }

  public static Int num(long i) {
    return new Int(i, Location.DUMMY, Location.DUMMY);
  }

  public static FloatingPoint num(double d) {
    return new FloatingPoint(d, Location.DUMMY, Location.DUMMY);
  }

  public static Bool bool(boolean b) {
    return new Bool(b, Location.DUMMY, Location.DUMMY);
  }

  public static Str str(String s) {
    return new Str(s, Location.DUMMY, Location.DUMMY);
  }

  public static Identifier name(String name) {
    return new Identifier(name, Location.DUMMY, Location.DUMMY);
  }

  public static Parenthesized paren(Node node) {
    return new Parenthesized(node, Location.DUMMY, Location.DUMMY);
  }

  public static IndexAccess access(Node target, Node index) {
    return new IndexAccess(target, index);
  }

  public static AttrAccess attr(Node target, Identifier attr) {
    return new AttrAccess(target, attr);
  }
}
