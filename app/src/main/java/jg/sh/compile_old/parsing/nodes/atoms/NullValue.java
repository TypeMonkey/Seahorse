package jg.sh.compile_old.parsing.nodes.atoms;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class NullValue extends ASTNode{

  public NullValue(int line, int column) {
    super(line, column);
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "NULL ~ at ln: "+getLine();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
