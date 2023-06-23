package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a binary operation expression: expr1 op expr2
 * where "op" is a TokenType in TokenType.binOps
 */
public class BinaryOpExpr extends Node {

  private final Operator op;
  private final Node left;
  private final Node right;

  public BinaryOpExpr(Operator op, Node left, Node right) {
    super(left.start, right.end);
    this.op = op;
    this.left = left;
    this.right = right;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitBinary(parentContext, this);
  }

  @Override
  public String repr() {
    return left.repr()+" "+op.repr()+" "+right.repr()+" [BIN EXPR]";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Operator getOp() {
    return op;
  }

  public Node getLeft() {
    return left;
  }

  public Node getRight() {
    return right;
  }
}
