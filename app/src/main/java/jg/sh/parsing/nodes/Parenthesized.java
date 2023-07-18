package jg.sh.parsing.nodes;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * An expression enclosed by a set of parenthesis
 * 
 * Format:
 *  ( expr )
 */
public class Parenthesized extends Node {

  private final Node inner;

  public Parenthesized(Node inner, Location start, Location end) {
    super(start, end);
    this.inner = inner;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitParenthesized(parentContext, this);
  }

  @Override
  public String repr() {
    return "( " + inner.repr() +" )";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Node getInner() {
    return inner;
  }
}
