package jg.sh.compile.parsing.nodes.atoms.constants;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

public class Int extends Constant<Long>{
  
  public Int(int line, int column, long value) {
    super(line, column, value);
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "INT ~ "+getValue();
  }

  @Override
  public boolean isLValue() {
    // TODO Auto-generated method stub
    return false;
  }
}
