package jg.sh.parsing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jg.sh.common.FunctionSignature;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.blocks.Block;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Str;

public class ParserTest {
  @Test
  public void testEmptyProgram() {
    String program = "";
    assertEquals(program.length(), 0);

    Tokenizer tokenizer = new Tokenizer(new StringReader(program), false);
    Parser parser = new Parser(tokenizer);

    try {
      Module prog = parser.parseProgram("SampleProgram");
      assertEquals(prog.getName(), "SampleProgram");
      assertEquals(prog.getStatements().size(), 0);
    } catch (ParseException e) {
      fail(e);
    }
  }

  @Test
  public void testSimpleProgram() {
    Reader src = new InputStreamReader(ParserTest.class.getResourceAsStream("/simpleProgram.shr"));

    Tokenizer tokenizer = new Tokenizer(src, false);
    Parser parser = new Parser(tokenizer);

    Module prog = null;
    try {
      prog = parser.parseProgram("SampleProgram");
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(prog.getName(), "SampleProgram");
    assertEquals(1, prog.getStatements().size());
    assertEquals(0, prog.getImports().size());

    final FuncDef mainFunc = (FuncDef) prog.getStatements().get(0).getExpr();
    assertTrue(mainFunc.getBoundName().getIdentifier().equals("main"));
    assertTrue(mainFunc.getParameters().size() == 2);

    Iterator<Entry<String, Parameter>> paramIterator = mainFunc.getParameters().entrySet().iterator();

    Entry<String, Parameter> firstParam = paramIterator.next();
    assertEquals("a", firstParam.getKey());
    assertEquals(null, firstParam.getValue().getInitValue());

    Entry<String, Parameter> secondParam = paramIterator.next();
    assertEquals("b", secondParam.getKey());
    assertEquals(false, secondParam.getValue().hasValue());

    assertTrue(mainFunc.getBody().getStatements().size() == 1);
    assertTrue(mainFunc.getBody().getStatements().get(0).getExpr() instanceof FuncCall);
    assertTrue(((FuncCall) mainFunc.getBody().getStatements().get(0).getExpr()).getArguments().length == 1);
    assertTrue(((FuncCall) mainFunc.getBody().getStatements().get(0).getExpr()).getArguments()[0].getArgument() instanceof Str);

    final Argument firstArg = ((FuncCall) mainFunc.getBody().getStatements().get(0).getExpr()).getArguments()[0];
    assertTrue(firstArg.getArgument() instanceof Str);
    assertTrue(((Str) firstArg.getArgument()).getValue().equals("hello world"));
  }

  @Test
  public void testComplexFuncs() {
    Reader src = new InputStreamReader(ParserTest.class.getResourceAsStream("/complexFuncs.shr"));

    Tokenizer tokenizer = new Tokenizer(src, false);
    Parser parser = new Parser(tokenizer);

    Module prog = null;
    try {
      prog = parser.parseProgram("SampleProgram");
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(1, prog.getStatements().size());    
    assertInstanceOf(FuncDef.class, prog.getStatements().get(0).getExpr());

    final FuncDef func = (FuncDef) prog.getStatements().get(0).getExpr();

    assertEquals("hello", func.getBoundName().getIdentifier());
    assertEquals(2, func.getSignature().getPositionalParamCount());
    assertTrue(func.getSignature().hasVariableParams());

    final Iterator<Entry<String, Parameter>> params = func.getParameters().entrySet().iterator();

    final Entry<String, Parameter> first = params.next();
    assertEquals("a", first.getValue().getName().getIdentifier());
    assertTrue(first.getValue().isConst());

    final Entry<String, Parameter> second = params.next();
    assertEquals("b", second.getValue().getName().getIdentifier());
    assertTrue(!second.getValue().isVarying());

    final Entry<String, Parameter> third = params.next();
    assertEquals("c", third.getValue().getName().getIdentifier());
    assertTrue(third.getValue().isVarying());

    assertFalse(params.hasNext());

    assertEquals(1, func.getBody().size());
    assertInstanceOf(ReturnStatement.class, func.getBody().get(0));
    final ReturnStatement returnStatement = (ReturnStatement) func.getBody().get(0);

    assertTrue(returnStatement.getExpr() != null);
    assertInstanceOf(BinaryOpExpr.class, returnStatement.getExpr());

    final BinaryOpExpr returnExpr = (BinaryOpExpr) returnStatement.getExpr();

    //first operand: a
    assertInstanceOf(Identifier.class, returnExpr.getLeft());
    final Identifier firstLeft = (Identifier) returnExpr.getLeft();
    assertEquals("a", firstLeft.getIdentifier());

    assertInstanceOf(BinaryOpExpr.class, returnExpr.getRight());
    final BinaryOpExpr firstRight = (BinaryOpExpr) returnExpr.getRight();

    //second operand: b
    assertInstanceOf(Identifier.class, firstRight.getLeft());
    final Identifier secondLeft = (Identifier) firstRight.getLeft();
    assertEquals("b", secondLeft.getIdentifier());

    //third operand: c[0]
    assertInstanceOf(IndexAccess.class, firstRight.getRight());
    final IndexAccess access = (IndexAccess) firstRight.getRight();
    assertEquals("c", assertAndCast(Identifier.class, access.getTarget()).getIdentifier());
    assertEquals(0, assertAndCast(Int.class, access.getIndex()).getValue());
  }

  private <T> T assertAndCast(Class<T> c, Object value) {
    assertInstanceOf(c, value);
    return (T) value;
  }

  private static FuncDef isFuncPresent(Collection<Statement> statements, String name) {
    for (Statement statement : statements) {
      if (statement.getExpr() instanceof FuncDef) {
        final FuncDef funcDef = (FuncDef) statement.getExpr();

        if (funcDef.getBoundName() != null && funcDef.getBoundName().getIdentifier().equals(name)) {
          return funcDef;
        }
      }
    }
    return null;
  }
}
