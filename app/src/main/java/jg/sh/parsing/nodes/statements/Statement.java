package jg.sh.parsing.nodes.statements;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Node;

/**
 * A statement is an isolated expression, terminated by a semicolon ';'.
 */
public class Statement extends Node {

  private final Node expr;

  public Statement(Node expr) {
    this(expr, expr.start, expr.end);
  }

  public Statement(Location start, Location end) {
    this(null, start, end);
  }

  public Statement(Node expr, Location start, Location end) {
    super(start, end);
    this.expr = expr;
  }

  public String repr() {
    return (expr != null ? expr.repr() : "") + ";";
  }

  public Location getStart() {
    return start;
  }

  public Location getEnd() {
    return end;
  }
  
  public Node getExpr() {
    return expr;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitStatement(parentContext, this);
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
