package jg.sh.compile.parsing.nodes.atoms.constants;

import jg.sh.compile.parsing.nodes.NodeVisitor;

public class Bool extends Constant<Boolean>{
  
  public Bool(int line, int column, boolean value) {
    super(line, column, value);
  }

  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "BOOL ~ "+getValue();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
