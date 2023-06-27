package jg.sh.parsing.nodes;

import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

/**
 * Represents an expression composed of two other expressions
 * operated on by some operator.
 * 
 * Format:
 * 
 * leftNode * rightNode 
 * 
 * where * is some binary operator
 */
public class BinaryOpExpr extends Node {

  private final Node left;
  private final Node right;
  private final Operator operator;

  public BinaryOpExpr(Node left, Node right, Operator operator) {
    super(left.start, right.end);
    this.left = left;
    this.right = right;
    this.operator = operator;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitBinaryExpr(parentContext, this);
  }

  @Override
  public String repr() {
    return left.repr() + operator.repr() + right.repr();
  }

  public Node getLeft() {
    return left;
  }

  public Node getRight() {
    return right;
  }

  public Operator getOperator() {
    return operator;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
