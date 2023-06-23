package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a shortened conditional expression: conditionalExpr ? trueExpr : falseExpr
 * where "conditionalExpr" is boolean-valued and the value of "trueExpr" if
 * the value of "conditionalExpr" is true, else the value of "falseExpr" is returned
 * 
 * Note: both "trueExpr" and "falseExpr" must be of the same type
 */
public class TernaryExpr extends Node {

  private final Node conditional, trueBranch, falseBranch;
  private final boolean isErrorTernary;

  public TernaryExpr(boolean isErrorTernary, Node conditional, Node trueBranch, Node falseBranch) {
    super(conditional.start, falseBranch.end);
    this.conditional = conditional;
    this.trueBranch = trueBranch;
    this.falseBranch = falseBranch;
    this.isErrorTernary = isErrorTernary;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitTernary(parentContext, this);
  }

  @Override
  public String repr() {
    return conditional + (isErrorTernary ? "!" : "?") + trueBranch + ":" + falseBranch;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Node getConditional() {
    return conditional;
  }

  public Node getTrueBranch() {
    return trueBranch;
  }
  
  public Node getFalseBranch() {
    return falseBranch;
  }

  public boolean isErrorTernary() {
    return isErrorTernary;
  }
}
