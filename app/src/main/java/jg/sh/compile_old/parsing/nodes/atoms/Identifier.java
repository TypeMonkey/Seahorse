package jg.sh.compile_old.parsing.nodes.atoms;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class Identifier extends ASTNode{

  private final String identifier;
  
  public Identifier(int line, int column, String identifier) {
    super(line, column);
    this.identifier = identifier;
  }
  
  public String getIdentifier() {
    return identifier;
  }
  
  @Override
  public int hashCode() {
    return identifier.hashCode();
  }
  
  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Identifier) {
      Identifier other = (Identifier) obj;
      return other.identifier.equals(identifier);
    }
    return false;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "IDEN ~ "+identifier;
  }

  @Override
  public boolean isLValue() {
    return true;
  }

}
