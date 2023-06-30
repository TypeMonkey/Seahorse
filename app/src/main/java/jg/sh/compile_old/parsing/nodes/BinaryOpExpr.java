package jg.sh.compile_old.parsing.nodes;

import jg.sh.common.OperatorKind;

public class BinaryOpExpr extends ASTNode{

  private final OperatorKind operator;
  private final ASTNode left;
  private final ASTNode right;
  
  public BinaryOpExpr(int line, int column, OperatorKind operator, ASTNode left, ASTNode right) {
    super(line, column);
    this.operator = operator;
    this.left = left;
    this.right = right;
  } 
  
  public OperatorKind getOperator() {
    return operator;
  }

  public ASTNode getLeft() {
    return left;
  }

  public ASTNode getRight() {
    return right;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "BIN ~ "+operator+" [LEFT: "+left+"]  [RIGHT: "+right+"]";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
