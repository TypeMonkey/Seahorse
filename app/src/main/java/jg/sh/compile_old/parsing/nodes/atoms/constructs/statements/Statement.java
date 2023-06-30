package jg.sh.compile_old.parsing.nodes.atoms.constructs.statements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;
import jg.sh.compile_old.parsing.nodes.ReservedWords;
import jg.sh.compile_old.parsing.nodes.atoms.Keyword;

/**
 * A statement is an expression terminated by a semicolon.
 * 
 * Statements are the atomic structure of a Seahorse program
 * @author Jose
 *
 */
public abstract class Statement extends ASTNode {

  private final Set<Keyword> modifiers;
  
  public Statement(int line, int column) {
    this(line, column, null);
  }
  
  public Statement(int line, int column, Keyword [] modifiers) {
    super(line, column);
    this.modifiers = modifiers == null ? new HashSet<>() : new HashSet<>(Arrays.asList(modifiers));
  }
  
  @Override
  public abstract void acceptVisitor(NodeVisitor visitor);

  @Override
  public abstract String toString();
  
  public boolean hasModifier(ReservedWords keyword) {
    return Keyword.toReservedWords(modifiers).contains(keyword);
  }
  
  public Set<Keyword> getModifiers() {
    return modifiers;
  }
  
  @Override
  public boolean isLValue() {
    return false;
  }
}
