package jg.sh.parsing;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import jg.sh.parsing.exceptions.TokenizerException;
import jg.sh.parsing.token.Token;
import jg.sh.parsing.token.TokenType;

public class Tokenizer implements Iterable<Token> {
  private PushbackReader source;
  private int currentLine;
  private int currentColumn;
  private List<Token> generatedTokens;

  // Flag that consume() sets true when it reaches a -1 value
  private boolean endOfFileReached;

  // Flag that next() sets true when it the next token is EOF
  private Token eofToken;

  /**
   * Cosntructs a Tokenizer
   * 
   * @param source     - the Reader to read characters from
   * @param keepTokens - whether to keep parsed Tokens (see: getGeneratedTokens())
   */
  public Tokenizer(Reader source, boolean keepTokens) {
    reset(source, keepTokens);
  }

  /**
   * Resets this Tokenizer with a new Reader
   * 
   * @param newSource  - the Reader to read characters from
   * @param keepTokens - whether to keep parsed Tokens (see: getGeneratedTokens())
   */
  public void reset(Reader newSource, boolean keepTokens) {
    this.source = new PushbackReader(newSource);
    this.currentLine = 1;
    this.currentColumn = 1;
    this.generatedTokens = keepTokens ? new ArrayList<>() : null;
  }

  public Reader getSource() {
    return source;
  }

  /**
   * Returns a List of Tokens that were parsed when calling nextToken(),
   * or null if this Tokenizer was set to with keepTokens = false
   * 
   * Note: the List returned by this method is a "live" list, meaning any Token 
   *       returned by nextToken() is added to the list returned by this method.
   * 
   * @return a List of Tokens that were parsed when calling nextToken(),
   *         or null if this Tokenizer was set to with keepTokens = false
   */
  public List<Token> getGeneratedTokens() {
    return generatedTokens;
  }

  /**
   * Returns the next token found in the source
   * 
   * @return the next Token
   * @throws IOException
   */
  public Token nextToken() throws IOException, TokenizerException {
    if (eofToken != null) {
      //This tokenizer reached the end of the file already and
      //subsequent calls to nextToken() are still being made
      System.out.println(" ---tokenzier.next() still being called");
      return eofToken;
    }

    while (true) {
      final int startColumn = currentColumn;
      final char intake = consumeChar();

      if (endOfFileReached) {
        eofToken = new Token("", TokenType.EOF, currentLine, currentColumn);
        System.out.println("*******tokenizer reached end!!!");
        return addToken(eofToken);
      }

      switch (intake) {
        case '(':
          return addToken(new Token("(", TokenType.LEFT_PAREN, currentLine, startColumn));
        case ')':
          return addToken(new Token(")", TokenType.RIGHT_PAREN, currentLine, startColumn));
        case '[':
          return addToken(new Token("[", TokenType.LEFT_SQ_BR, currentLine, startColumn));
        case ']':
          return addToken(new Token("]", TokenType.RIGHT_SQ_BR, currentLine, startColumn));
        case '{':
          return addToken(new Token("{", TokenType.LEFT_CURL, currentLine, startColumn));
        case '}':
          return addToken(new Token("}", TokenType.RIGHT_CURL, currentLine, startColumn));
        case '?':
          return addToken(new Token("?", TokenType.QUESTION, currentLine, startColumn));
        case '!': {
          return addToken(checkNext('=') ? new Token("!=", TokenType.NOT_EQ, currentLine, startColumn, currentColumn)
              : new Token("=", TokenType.EQUAL, currentLine, startColumn));
        }
        case '+': {
          return addToken(checkNext('=') ? new Token("+=", TokenType.EQ_ADD, currentLine, startColumn, currentColumn)
              : new Token("+", TokenType.PLUS, currentLine, startColumn));
        }
        case '-': {
          if (peek() == '>') {
            consumeChar();
            return addToken(new Token("->", TokenType.ARROW, currentLine, startColumn, currentColumn));
          }

          return addToken(checkNext('=') ? new Token("-=", TokenType.EQ_MIN, currentLine, startColumn, currentColumn)
              : new Token("-", TokenType.MINUS, currentLine, startColumn));
        }
        case '*': {
          if (peek() == '/') {
            consumeChar();
            return addToken(new Token("*/", TokenType.COMMENT_BLOCK_END, currentLine, startColumn, currentColumn));
          }

          return addToken(checkNext('=') ? new Token("*=", TokenType.EQ_MULT, currentLine, startColumn, currentColumn)
              : new Token("*", TokenType.MULT, currentLine, startColumn));
        }
        case '/': {
          if (peek() == '*') {
            consumeChar();
            return addToken(new Token("/*", TokenType.COMMENT_BLOCK_START, currentLine, startColumn, currentColumn));
          } else if (peek() == '/') {
            // this is a single line comment
            consumeChar();

            while (!endOfFileReached) {
              if (peek() == '\n') {
                break;
              }
              consumeChar();
            }

            // Ignore all comments until the next new line
            continue;
          } else {
            return addToken(checkNext('=') ? new Token("/=", TokenType.EQ_DIV, currentLine, startColumn, currentColumn)
                : new Token("/", TokenType.DIV, currentLine, startColumn));
          }
        }
        case '^': {
          return addToken(checkNext('=') ? new Token("^=", TokenType.EQ_EXPO, currentLine, startColumn, currentColumn)
              : new Token("^", TokenType.EXPONENT, currentLine, startColumn));
        }
        case '=': {
          return addToken(new Token("=", TokenType.EQUAL, currentLine, startColumn));
        }
        case '&': {
          return addToken(checkNext('&') ? new Token("&&", TokenType.BOOL_AND, currentLine, startColumn, currentColumn)
              : new Token("&", TokenType.AND, currentLine, startColumn));
        }
        case '|': {
          return addToken(checkNext('|') ? new Token("||", TokenType.BOOL_OR, currentLine, startColumn, currentColumn)
              : new Token("|", TokenType.OR, currentLine, startColumn));
        }
        case '%': {
          return addToken(checkNext('=') ? new Token("%=", TokenType.EQ_MOD, currentLine, startColumn, currentColumn)
              : new Token("%", TokenType.MOD, currentLine, startColumn));
        }
        case '<': {
          return addToken(checkNext('<') ? new Token("<=", TokenType.LS_EQ, currentLine, startColumn, currentColumn)
              : new Token("<", TokenType.LESS, currentLine, startColumn));
        }
        case '>': {
          return addToken(checkNext('>') ? new Token(">=", TokenType.GR_EQ, currentLine, startColumn, currentColumn)
              : new Token(">", TokenType.GREAT, currentLine, startColumn));
        }
        case '"': {
          String literal = "";

          while (!endOfFileReached) {
            if (peek() == '"') {
              break;
            } else if (peek() == '\n') {
              consumeChar();
              throw new TokenizerException("String literals must be contained in a single line", currentLine, currentColumn);
            }
            literal += consumeChar();
          }

          // Consume the last double quote
          if (consumeChar() != '"') {
            throw new TokenizerException("Unexpected: EOF reached before string's end", currentLine, currentColumn);
          }

          return addToken(new Token(literal, TokenType.STRING, startColumn, currentColumn));
        }
        case ':': {
          return addToken(
              checkNext('=') ? new Token(":=", TokenType.ASSIGNMENT, currentLine, startColumn, currentColumn)
                  : new Token(":", TokenType.COLON, currentLine, startColumn));
        }
        case ';': {
          return addToken(new Token(";", TokenType.SEMICOLON, currentLine, currentColumn));
        }
        case ',': {
          return addToken(new Token(";", TokenType.COMMA, currentLine, currentColumn));
        }
        case '.': {
          return addToken(new Token(".", TokenType.DOT, currentLine, currentColumn));
        }

        // Ignore whitespace
        case ' ':
        case '\r':
        case '\t':
          continue;
        case '\n': {
          System.out.println("  ----- new line! " + currentLine);
          currentColumn = 1;
          currentLine++;
          continue;
        }
        default: {
          if (Character.isDigit(intake)) {
            return addToken(consumeNumber(intake, startColumn));
          } else if (Character.isLetter(intake)) {
            return addToken(consumePotentialIdentifier(intake, startColumn));
          } else {
            throw new TokenizerException("Unknown token '"+intake+"' ("+Integer.valueOf(intake)+")", currentLine, currentColumn);
          }
        }
      }
    }
  }

  public boolean hasNext() {
    return eofToken == null;
  }

  // Utility Methods (START)
  private Token consumePotentialIdentifier(final char initialChar, final int startColumn) throws IOException {
    String word = "" + initialChar;

    while (!isEnd() && isValidIdentifierChar(peek())) {
      word += consumeChar();
    }

    if (TokenType.isKeyword(word)) {
      return new Token(word, TokenType.valueOf(word.toUpperCase()), currentLine, startColumn, currentColumn);
    }
    return new Token(word, TokenType.IDENTIFIER, currentLine, startColumn, currentColumn);
  }

  private boolean isValidIdentifierChar(char ch) {
    return Character.isDigit(ch) || Character.isLetter(ch) || ch == '_' || ch == '$';
  }

  private Token consumeNumber(final char initialChar, final int startColumn) throws IOException {
    // Either an int or a decimal

    String numerical = "" + initialChar;
    boolean isInt = true;

    // will only stop if a non-digit (0-9) character is found
    while (!isEnd() && Character.isDigit(peek())) {
      numerical += consumeChar();
    }

    // Check if "." was encountered
    if (peek() == '.') {
      numerical += consumeChar(); // consume "."
      if (Character.isDigit(peek())) {
        while (!isEnd() && Character.isDigit(peek())) {
          numerical += consumeChar();
        }
        isInt = false;
      } else {
        pushback('.');
      }
    }

    return isInt ? new Token(numerical, TokenType.INTEGER, currentLine, startColumn, currentColumn)
        : new Token(numerical, TokenType.DECIMAL, currentLine, startColumn, currentColumn);
  }

  private char consumeChar() throws IOException {
    int currChar = source.read();
    currentColumn++;

    // end of file reached
    if (currChar == -1) {
      endOfFileReached = true;
    }

    return (char) currChar;
  }

  private boolean isEnd() throws IOException {
    return eofToken != null;
  }

  private boolean checkNext(char expected) throws IOException {
    if (!isEnd()) {
      final char nextChar = consumeChar();

      if (expected == nextChar) {
        currentColumn++;
        return true;
      }

      pushback(nextChar);
    }
    return false;
  }

  private char peek() throws IOException {
    if (isEnd()) {
      return '\0';
    } else {
      final char nextChar = consumeChar();
      pushback(nextChar);
      return nextChar;
    }
  }

  private void pushback(char ch) throws IOException {
    source.unread(ch);
    currentColumn--;
  }

  private Token addToken(Token token) {
    if (generatedTokens != null) {
      generatedTokens.add(token);
    }
    return token;
  }

  // Utility Methods (END)

  @Override
  public TokenizerIterator iterator() {
    return new TokenizerIterator(this, false);
  }
}
