package jg.sh.compile.parsing.nodes.atoms.constructs.blocks;

import java.util.List;

import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;

/**
 * Represents a block of statements, bounded syntactically
 * by opening and closing curly braces.
 * 
 * Empty StatementGroups are discarded after parsing.
 * @author Jose
 *
 */
public abstract class Block extends Statement{

  private final Keyword signifier;
  private final List<Statement> statements;
  
  public Block(int line, int column, Keyword signifier, List<Statement> statements) {
    super(line, column);
    this.signifier = signifier;
    this.statements = statements;
  }
  
  public Keyword getSignifier() {
    return signifier;
  }

  public List<Statement> getStatements() {
    return statements;
  }
  
  @Override
  public abstract void acceptVisitor(NodeVisitor visitor);

  @Override
  public String toString() {
    String stateStrings = "";
    for (Statement statement : statements) {
      stateStrings += "   "+statement.toString() + System.lineSeparator();
    }
    return "S_GROUP: "+stateStrings;
  }
}
