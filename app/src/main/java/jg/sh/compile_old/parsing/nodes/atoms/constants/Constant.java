package jg.sh.compile_old.parsing.nodes.atoms.constants;

import jg.sh.compile_old.parsing.nodes.ASTNode;

/**
 * Represents string literals, numerical and boolean cosntants
 * @author Jose
 *
 * @param <V> - the type of the constant
 */
public abstract class Constant<V> extends ASTNode{

  private final V value;
  
  public Constant(int line, int column, V value) {
    super(line, column);
    this.value = value;
  }

  public V getValue() {
    return value;
  }
  
  public abstract String toString();
}
