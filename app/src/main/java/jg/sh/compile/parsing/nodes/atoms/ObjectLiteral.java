package jg.sh.compile.parsing.nodes.atoms;

import java.util.Map;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

public class ObjectLiteral extends ASTNode{

  private final Map<String, ASTNode> keyValPairs;
  
  public ObjectLiteral(int line, int column, Map<String, ASTNode> keyValPairs) {
    super(line, column);
    this.keyValPairs = keyValPairs;
  }

  public Map<String, ASTNode> getKeyValPairs() {
    return keyValPairs;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "DICT ~ "+keyValPairs;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
