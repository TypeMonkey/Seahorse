package jg.sh.compile_old.parsing.nodes.atoms;

import java.util.Arrays;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class ArrayLiteral extends ASTNode{

  private final ASTNode [] arrayValues;
  
  public ArrayLiteral(int line, int column, ASTNode [] arrayValues) {
    super(line, column);
    this.arrayValues = arrayValues;
  }
  
  public ASTNode[] getArrayValues() {
    return arrayValues;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ARRAY_LIT ~ "+Arrays.toString(arrayValues);
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
