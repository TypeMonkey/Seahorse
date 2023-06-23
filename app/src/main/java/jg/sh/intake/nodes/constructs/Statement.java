package jg.sh.intake.nodes.constructs;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * A Statement is logical grouping of expressions, ending with a semicolon
 */
public class Statement extends Node {

  private final Node expr;

  public Statement(Node expr) {
    super(expr.start, expr.end);
    this.expr = expr;
  }

  protected Statement(Location start, Location end) {
    super(start, end);
    this.expr = null;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitStatement(parentContext, this);
  }

  @Override
  public String repr() {
    return expr.repr()+" ;";
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  public Node getExpr() {
    return expr;
  }
}
