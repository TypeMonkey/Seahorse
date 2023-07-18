package jg.sh.parsing.nodes.statements;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;

public class ThrowStatement extends Statement {

  private final Node exception;

  public ThrowStatement(Keyword returnKeyword, Node exception, Location end) {
    super(returnKeyword.start, end);
    this.exception = exception;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitThrowStatement(parentContext, this);
  } 
  
  public Node getException() {
    return exception;
  }
}
