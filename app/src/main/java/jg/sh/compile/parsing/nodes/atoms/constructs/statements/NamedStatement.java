package jg.sh.compile.parsing.nodes.atoms.constructs.statements;

import java.util.Set;

import jg.sh.compile.parsing.nodes.atoms.Keyword;

/**
 * Represents a named statement - such as variables, functions and data definitions
 * 
 * The other methods - getLine(), getColumn() and getModifiers() - are all methods
 * that are meant to align with the Statement and ASTNode abstract classes
 * 
 * @author Jose
 * 
 * @deprecated
 */
public interface NamedStatement {
  
  /**
   * Returns the name designated for this statement
   * @return the name designated for this statement
   */
  public String getName();
  
  public int getLine();
  
  public int getColumn();
  
  public Set<Keyword> getModifiers();
}
