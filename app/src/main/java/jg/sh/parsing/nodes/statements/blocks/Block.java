package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.statements.Statement;

/**
 * A sequential grouping of statements
 */
public class Block extends Statement {

  protected final List<Statement> statements;

  public Block(List<Statement> statements, Location start, Location end) {
    super(start, end);
    this.statements = statements;
  }

  public List<Statement> getStatements() {
    return statements;
  }
}
