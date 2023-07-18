package jg.sh.parsing;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jg.sh.parsing.token.Token;
import jg.sh.parsing.token.TokenType;

public class TokenizerTest {
  @Test
  public void emptySrcTest() {
    Tokenizer tokenizer = new Tokenizer(new StringReader(""), false);

    final TokenType [] tokens = toArray(tokenizer);
    final TokenType [] expected = toArray(TokenType.EOF);

    assertEquals(tokens.length, expected.length);
    assertArrayEquals(tokens, expected);
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void simpleOneLineTest() {
    StringBuilder src = new StringBuilder();
    src.append("var v := 50;");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    assertArrayEquals(toArray(TokenType.VAR,
        TokenType.IDENTIFIER,
        TokenType.ASSIGNMENT,
        TokenType.INTEGER,
        TokenType.SEMICOLON,
        TokenType.EOF), toArray(tokenizer));
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void singleLineTest2() {
    StringBuilder src = new StringBuilder();
    src.append("func int(what the freak) {}");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    assertArrayEquals(toArray(TokenType.FUNC,
        TokenType.IDENTIFIER,
        TokenType.LEFT_PAREN,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.RIGHT_PAREN,
        TokenType.LEFT_CURL,
        TokenType.RIGHT_CURL,
        TokenType.EOF), toArray(tokenizer));
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void keywordTest() {
    String allKeywords = TokenType.keywords.stream().collect(Collectors.joining(" "));

    Tokenizer tokenizer = new Tokenizer(new StringReader(allKeywords), false);
    TokenType[] allTypes = toArray(tokenizer);

    for (int i = 0; i < allTypes.length - 1; i++) {
      try {
        TokenType.valueOf(allTypes[i].name().toUpperCase());
      } catch (Exception e) {
        fail("unmatched keyword: " + allTypes[i]);
      }
    }

    assertEquals(allTypes[allTypes.length - 1], TokenType.EOF);
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void commentTest() {
    StringBuilder src = new StringBuilder();
    src.append("//hello leave me be \n");
    src.append("func int(what the freak) {}");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    assertArrayEquals(toArray(TokenType.FUNC,
        TokenType.IDENTIFIER,
        TokenType.LEFT_PAREN,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.RIGHT_PAREN,
        TokenType.LEFT_CURL,
        TokenType.RIGHT_CURL,
        TokenType.EOF), toArray(tokenizer));
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void commentTest2() {
    StringBuilder src = new StringBuilder();
    src.append("//hello leave me be \n");
    src.append("func int(what the freak) //{}\n");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    assertArrayEquals(toArray(TokenType.FUNC,
        TokenType.IDENTIFIER,
        TokenType.LEFT_PAREN,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.RIGHT_PAREN,
        TokenType.EOF), toArray(tokenizer));
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void commentTestNoNewLine() {
    StringBuilder src = new StringBuilder();
    src.append("//hello leave me be \n");

    /*
     * This and the previous test should result the same
     */
    src.append("func int(what the freak) //{}");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    assertArrayEquals(toArray(TokenType.FUNC,
        TokenType.IDENTIFIER,
        TokenType.LEFT_PAREN,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.IDENTIFIER,
        TokenType.RIGHT_PAREN,
        TokenType.EOF), toArray(tokenizer));
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void commentBlockTest() {
    StringBuilder src = new StringBuilder();
    src.append("/*hello leave me be */");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    TokenType[] result = toArray(tokenizer);
    assertTrue(result.length == 1);
    assertEquals(result[0], TokenType.EOF);
    assertFalse(tokenizer.hasNext());
  }

  @Test
  public void commentBlockTest2() {
    StringBuilder src = new StringBuilder();
    src.append("/*hello leave me be */howdy");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    assertTrue(tokens.hasNext());
    final Token identToken = tokens.next();
    assertTrue(tokens.hasNext());
    final Token eofToken = tokens.next();
    assertFalse(tokens.hasNext());
    assertEquals(identToken.getType(), TokenType.IDENTIFIER);
    assertEquals(identToken.getContent(), "howdy");
    assertEquals(eofToken.getType(), TokenType.EOF);
  }

  @Test
  public void commentBlockTest3() {
    StringBuilder src = new StringBuilder();
    src.append("/* hello leave \n");
    src.append("let f:int = 10; */ \n");
    src.append("howdy");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    assertTrue(tokens.hasNext());
    final Token identToken = tokens.next();
    assertTrue(tokens.hasNext());
    final Token eofToken = tokens.next();
    assertFalse(tokens.hasNext());
    assertEquals(identToken.getType(), TokenType.IDENTIFIER);
    assertEquals(identToken.getContent(), "howdy");
    assertEquals(eofToken.getType(), TokenType.EOF);
  }

  @Test
  public void commentBlockTest4() {
    StringBuilder src = new StringBuilder();
    src.append("/* hello leave \n");
    src.append("let f:int = 10;\n");
    src.append("*/             \n");
    src.append("howdy");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    assertTrue(tokens.hasNext());
    final Token identToken = tokens.next();
    assertTrue(tokens.hasNext());
    final Token eofToken = tokens.next();
    assertFalse(tokens.hasNext());
    assertEquals(identToken.getType(), TokenType.IDENTIFIER);
    assertEquals(identToken.getContent(), "howdy");
    assertEquals(eofToken.getType(), TokenType.EOF);
  }

  @Test
  public void simpleStringTest() {
    StringBuilder src = new StringBuilder();
    src.append('"' + "I'm a string" + '"');

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    assertTrue(tokens.hasNext());
    final Token stringToken = tokens.next();
    assertTrue(tokens.hasNext());
    final Token eofToken = tokens.next();
    assertFalse(tokens.hasNext());
    assertEquals(stringToken.getType(), TokenType.STRING);
    assertEquals(stringToken.getContent(), "I'm a string");

    assertEquals(eofToken.getType(), TokenType.EOF);
  }

  @Test
  public void stringNewLineTest() {
    StringBuilder src = new StringBuilder();
    src.append('"' + "I'm a \n string" + '"');

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    Assertions.assertThrows(IllegalStateException.class, () -> {
      assertTrue(tokens.hasNext());
      final Token stringToken = tokens.next();
      assertTrue(tokens.hasNext());
      final Token eofToken = tokens.next();
      assertFalse(tokens.hasNext());
      assertEquals(stringToken.getType(), TokenType.STRING);
      assertEquals(stringToken.getContent(), "I'm a string");
      assertEquals(eofToken.getType(), TokenType.EOF);
    });
  }

  @Test
  public void stringNewLineTest2() {
    StringBuilder src = new StringBuilder();
    src.append('"' + "I'm a string");

    Tokenizer tokenizer = new Tokenizer(new StringReader(src.toString()), false);
    final Iterator<Token> tokens = tokenizer.iterator();

    Assertions.assertThrows(IllegalStateException.class, () -> {
      assertTrue(tokens.hasNext());
      final Token stringToken = tokens.next();
      assertTrue(tokens.hasNext());
      final Token eofToken = tokens.next();
      assertFalse(tokens.hasNext());
      assertEquals(stringToken.getType(), TokenType.STRING);
      assertEquals(stringToken.getContent(), "I'm a string");
      assertEquals(eofToken.getType(), TokenType.EOF);
    });
  }

  static TokenType[] toArray(Iterable<Token> tokenStream) {
    ArrayList<TokenType> types = new ArrayList<>();

    for (Token t : tokenStream) {
      types.add(t.getType());
    }

    return types.toArray(new TokenType[types.size()]);
  }

  static TokenType[] toArray(TokenType... tokens) {
    return tokens;
  }
}
