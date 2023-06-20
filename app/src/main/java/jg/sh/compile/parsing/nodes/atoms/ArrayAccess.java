package jg.sh.compile.parsing.nodes.atoms;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

public class ArrayAccess extends ASTNode {
  
  private final ASTNode target;
  private final ASTNode indexValue;
  
  public ArrayAccess(int line, int column, ASTNode target, ASTNode indexValue) {
    super(line, column);
    this.target = target;
    this.indexValue = indexValue;
  }
  
  public ASTNode getIndexValue() {
    return indexValue;
  }
  
  public ASTNode getTarget() {
    return target;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
   
  @Override
  public String toString() {
    return "ARR_ACC ~ ("+target+") ["+indexValue+"]";
  }

  @Override
  public boolean isLValue() {
    return true;
  }
}
