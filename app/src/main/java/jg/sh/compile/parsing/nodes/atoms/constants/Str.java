package jg.sh.compile.parsing.nodes.atoms.constants;

import jg.sh.compile.parsing.nodes.NodeVisitor;

public class Str extends Constant<String>{
  
  public Str(int line, int column, String strLiteral) {
    super(line, column, strLiteral);
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "STR ~ |"+getValue()+"|";
  }

  @Override
  public boolean isLValue() {
    // TODO Auto-generated method stub
    return false;
  }
}
