package jg.sh.parsing.nodes;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.statements.Statement;

public class ThrowStatement extends Statement {

  private final Node exception;

  public ThrowStatement(Keyword returnKeyword, Node exception, Location end) {
    super(returnKeyword.start, end);
    this.exception = exception;
  }
  
  public Node getException() {
    return exception;
  }
}
