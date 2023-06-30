package jg.sh.compile_old.parsing.nodes.atoms;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class AttrAccess extends ASTNode{

  private final ASTNode target;
  private final String attrName;
  
  public AttrAccess(int line, int column, ASTNode target, String attrName) {
    super(line, column);
    this.target = target;
    this.attrName = attrName;
  }

  public ASTNode getTarget() {
    return target;
  }
  
  public String getAttrName() {
    return attrName;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "ATTR ~ ("+target+") -> "+attrName;
  }

  @Override
  public boolean isLValue() {
    return true;
  }
}
