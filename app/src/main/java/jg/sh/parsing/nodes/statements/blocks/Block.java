package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;
import java.util.stream.Collectors;

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

  @Override
  public String repr() {
    return statements.stream()
                      .map(Statement::repr)
                      .collect(Collectors.joining(System.lineSeparator()));
  }

  public Statement get(int index) {
    return statements.get(index);
  }

  public List<Statement> getStatements() {
    return statements;
  }

  public int size() {
    return statements.size();
  }
}
