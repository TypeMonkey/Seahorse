package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

public class UnaryExpr extends Node {

  private final Operator unaryOp;
  private final Node target;

  public UnaryExpr(Operator unaryOp, Node target) {
    super(unaryOp.start, target.end);
    this.unaryOp = unaryOp;
    this.target = target;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitUnary(parentContext, this);
  }

  @Override
  public String repr() {
    return unaryOp.repr()+target;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Node getTarget() {
    return target;
  }

  public Operator getUnaryOp() {
    return unaryOp;
  }
}
