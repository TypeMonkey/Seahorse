package jg.sh.compile_old.parsing.nodes.atoms.constants;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class FloatingPoint extends Constant<Double>{
  
  public FloatingPoint(int line, int column, double value) {
    super(line, column, value);
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "FLOAT ~ "+getValue();
  }

  @Override
  public boolean isLValue() {
    // TODO Auto-generated method stub
    return false;
  }
}
