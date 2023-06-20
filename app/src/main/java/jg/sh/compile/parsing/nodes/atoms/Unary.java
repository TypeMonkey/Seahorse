package jg.sh.compile.parsing.nodes.atoms;

import jg.sh.common.OperatorKind;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

public class Unary extends ASTNode{

  private final OperatorKind op;
  private final ASTNode target;
  
  public Unary(int line, int column, OperatorKind op, ASTNode target) {
    super(line, column);
    this.op = op;
    this.target = target;
  }

  public ASTNode getTarget() {
    return target;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  public OperatorKind getOp() {
    return op;
  }
  
  @Override
  public String toString() {
    return "UNARY ~ "+op+" <-> "+target;
  }
  
  public static OperatorKind stringToUnaryOp(String unaryOp) {
    if (unaryOp.equals("!")) {
      return OperatorKind.NOT;
    }
    else if (unaryOp.equals("-")) {
      return OperatorKind.MINUS;
    }
    return null;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
