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
import java.util.Collections;
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
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.blocks.Block;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Str;

import static jg.sh.parsing.utils.ParseTestUtils.*;

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
      System.out.println(prog);
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(2, prog.getStatements().size());    

    final FuncDef func = assertHasFunc("hello", 
                                       2, 
                                       Collections.emptySet(), 
                                       true, 
                                       prog.getStatements());

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
    assertEquals(Op.PLUS, returnExpr.getOperator().getOp());

    //first operand: a
    assertName("a", returnExpr.getLeft());

    final BinaryOpExpr firstRight = assertAndCast(BinaryOpExpr.class, returnExpr.getRight());
    assertEquals(Op.PLUS, firstRight.getOperator().getOp());

    //second operand: b
    assertName("b", firstRight.getLeft());

    //third operand: c[0]
    assertIndex("c", 0, assertNest(IndexAccess.class, 1, assertAndCast(Parenthesized.class, firstRight.getRight())));

    //Now, test the other top level statement
    final VarDeclr varDeclr = assertHasVar("b", true, false, true, prog.getStatements());
    
    final BinaryOpExpr value = assertAndCast(BinaryOpExpr.class, varDeclr.getInitialValue());
    assertEquals(Op.PLUS, value.getOperator().getOp());

    final Parenthesized fLeft = assertAndCast(Parenthesized.class, value.getLeft());
    final Parenthesized fRight = assertAndCast(Parenthesized.class, value.getRight());

    //check first left
    assertInt(10, assertNest(BinaryOpExpr.class, 1, fLeft).getLeft());
    assertEquals(Op.MINUS, assertNest(BinaryOpExpr.class, 1, fLeft).getOperator().getOp());
    assertInt(90, assertAndCast(BinaryOpExpr.class, assertNest(BinaryOpExpr.class, 1, fLeft).getRight()).getLeft());

    

  }

}
