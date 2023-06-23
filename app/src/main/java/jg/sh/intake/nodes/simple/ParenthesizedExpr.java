package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

public class ParenthesizedExpr extends Node {

  private final Node target;

  public ParenthesizedExpr(Node target, Location start, Location end) {
    super(start, end);
    this.target = target;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitParenthesized(parentContext, this);
  }

  @Override
  public String repr() {
    return "( "+target.repr()+" )";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
