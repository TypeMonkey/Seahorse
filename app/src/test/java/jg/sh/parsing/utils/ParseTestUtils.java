package jg.sh.parsing.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Collection;
import java.util.Set;

import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Str;
import jg.sh.parsing.token.TokenType;

public final class ParseTestUtils {
  
  public static void assertSignature(int positionalCount, 
                                     Set<String> optionals, 
                                     boolean hasVariedParam, 
                                     FunctionSignature signature) {
    assertEquals(positionalCount, signature.getPositionalParamCount());
    assertTrue(signature.getKeywordParams().containsAll(optionals), "Expected optional parameters missing in signature.");
    assertTrue(optionals.containsAll(signature.getKeywordParams()), "Declared parameters missing in expected.");
    assertEquals(hasVariedParam, signature.hasVariableParams());
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
      if (statement.getExpr() instanceof VarDeclr) {
        final VarDeclr varDeclr = (VarDeclr) statement.getExpr();
        if (varDeclr.getName().getIdentifier().equals(name) &&
            varDeclr.isConst() == isConst &&
            varDeclr.hasInitialValue() == hasInitValue &&
            Keyword.hasKeyword(TokenType.CONST, varDeclr.getDescriptors())) {
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
    assertInstanceOf(c, value);
    return (T) value;
  }

  //Wrapper methods
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
}
