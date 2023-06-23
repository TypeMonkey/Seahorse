package jg.sh.intake.token;

public class Token {

  private final String content;
  private final TokenType type;

  private final int lineNumber;
  private final int startCol;
  private final int endCol;

  public Token(String content, TokenType type, int lineNumber, int column) {
    this(content, type, lineNumber, column, column);
  }

  public Token(String content, TokenType type, int lineNumber, int startCol, int endCol) {
    this.content = content;
    this.type = type;
    this.lineNumber = lineNumber;
    this.startCol = startCol;
    this.endCol = endCol;
  }

  public TokenType getType() {
    return type;
  }

  public int getLineNumber() {
    return lineNumber;
  }
    
  public int getStartCol() {
    return startCol;
  }

  public int getEndCol() {
    return endCol;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return content+" ("+type+") ["+startCol+","+endCol+"] , ln "+lineNumber;
  }
}
