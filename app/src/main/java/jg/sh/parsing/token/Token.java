package jg.sh.parsing.token;

import jg.sh.common.Location;

public class Token {

  private final String content;
  private final TokenType type;

  private final Location start;
  private final Location end;

  public Token(String content, TokenType type, int lineNumber, int column) {
    this(content, type, lineNumber, column, column);
  }

  public Token(String content, TokenType type, int lineNumber, int startCol, int endCol) {
    this.content = content;
    this.type = type;
    this.start = new Location(lineNumber, startCol);
    this.end = new Location(lineNumber, endCol);
  }

  public TokenType getType() {
    return type;
  }

  public int getLineNumber() {
    return start.line;
  }
    
  public int getStartCol() {
    return start.column;
  }

  public int getEndCol() {
    return end.column;
  }

  public Location getEnd() {
    return end;
  }

  public Location getStart() {
    return start;
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return content+" ("+type+") ["+getStartCol()+","+getEndCol()+"] , ln "+getLineNumber();
  }
}
