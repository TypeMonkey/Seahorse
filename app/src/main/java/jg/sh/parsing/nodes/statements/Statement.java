package jg.sh.parsing.nodes.statements;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.Node;

/**
 * A statement is an isolated expression, terminated by a semicolon ';'.
 */
public class Statement {

  private final Node expr;
  private final Location start;
  private final Location end;

  public Statement(Node expr) {
    this(expr, expr.start, expr.end);
  }

  public Statement(Location start, Location end) {
    this(null, start, end);
  }

  public Statement(Node expr, Location start, Location end) {
    this.expr = expr;
    this.start = start;
    this.end = end;
  }

  public String repr() {
    return (expr != null ? expr.repr() : "") + ";";
  }

  public Location getStart() {
    return start;
  }

  public Location getEnd() {
    return end;
  }
  
  public Node getExpr() {
    return expr;
  }
}
