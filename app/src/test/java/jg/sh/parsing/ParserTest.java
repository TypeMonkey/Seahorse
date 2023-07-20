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

import org.junit.jupiter.api.Test;

import jg.sh.parsing.exceptions.ParseException;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.VarDeclr;
import jg.sh.parsing.nodes.values.Str;

import static jg.sh.parsing.utils.ParseTestUtils.*;
import static jg.sh.parsing.nodes.Operator.Op.*;

public class ParserTest {

  @Test
  public void testEmptyProgram() {
    String program = "";
    assertEquals(program.length(), 0);

    Tokenizer tokenizer = new Tokenizer(new StringReader(program), false);
    Parser parser = new Parser(tokenizer, "SampleProgram");

    try {
      Module prog = parser.parseProgram();
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
    Parser parser = new Parser(tokenizer, "SampleProgram");

    Module prog = null;
    try {
      prog = parser.parseProgram();
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(prog.getName(), "SampleProgram");
    assertEquals(1, prog.getStatements().size());
    assertEquals(1, prog.getImports().size());

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
    Parser parser = new Parser(tokenizer, "SampleProgram");

    Module prog = null;
    try {
      prog = parser.parseProgram();
      System.out.println(prog);
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(2, prog.getStatements().size());   
    System.out.println("var: "+prog.getStatements().get(1)); 

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
    final BinaryOpExpr expectedReturn = bin(bin(name("a"), PLUS, name("b")), PLUS, paren(access(name("c"), num(0))));
    assertNodeEquals(expectedReturn, returnExpr);

    //Now, test the other top level statement
    final VarDeclr varDeclr = assertHasVar("b", true, false, true, prog.getStatements());
    
    final BinaryOpExpr expectedValue = bin(paren(
                                            bin(
                                              bin(
                                                num(10), 
                                                MINUS, 
                                                num(90)), 
                                              PLUS, 
                                              paren(
                                                bin(unary(MINUS, num(5366)), 
                                                    PLUS, 
                                                    num(5.5))))),
                                           PLUS, 
                                           paren(
                                            bin(
                                              bin(
                                                num(50), 
                                                MULT, 
                                                num(95)), 
                                              DIV, 
                                              paren(num(5.5)))));
    assertBinEquals(expectedValue, varDeclr.getInitialValue());
  }

  @Test
  public void testNestedArith() {
    final String src = "const sample := 10 + 85 - 9.63 * 87 / 74 - func anon() {return -1;}() + 50;";

    //(((10 + 85) - ((9.63 * 87) / 74)) - func anon() {return -1;}()) + 50

    Tokenizer tokenizer = new Tokenizer(new StringReader(src), false);
    Parser parser = new Parser(tokenizer, "SampleProgram");

    Module prog = null;
    try {
      prog = parser.parseProgram();
    } catch (ParseException e) {
      fail(e);
    }

    assertEquals(1, prog.getStatements().size());
    final VarDeclr varDeclr = assertAndCast(VarDeclr.class, prog.getStatements().get(0));
    assertEquals("sample", varDeclr.getName().getIdentifier());
    assertTrue(varDeclr.isConst());
    assertTrue(varDeclr.hasInitialValue());

    final BinaryOpExpr expr = assertAndCast(BinaryOpExpr.class, varDeclr.getInitialValue());
    final BinaryOpExpr expected = bin(bin(
                                        bin(
                                            bin(
                                              num(10), 
                                              PLUS, 
                                              num(85)), 
                                            MINUS, 
                                            bin(
                                              bin(
                                                num(9.63), 
                                                MULT, 
                                                num(87)), 
                                              DIV, 
                                            num(74))), 
                                          MINUS, 
                                          call(simpleFunc("anon", unary(MINUS, num(1))))), 
                                      PLUS, 
                                      num(50));
    assertNodeEquals(expected, expr);
  }

}
