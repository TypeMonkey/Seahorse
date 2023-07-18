package jg.sh.parsing.nodes;

import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

public class UnaryExpr extends Node {

  private final Operator unaryOperator;
  private final Node target;

  public UnaryExpr(Operator unaryOp, Node target) {
    super(unaryOp.start, target.end);
    this.target = target;
    this.unaryOperator = unaryOp;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitUnary(parentContext, this);
  }

  public Node getTarget() {
    return target;
  }

  public Operator getOperator() {
    return unaryOperator;
  }

  @Override
  public String repr() {
    return unaryOperator.repr()+" "+target.repr();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
