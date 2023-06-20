package jg.sh.compile.parsing.nodes.atoms;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

/**
 * Expression that's grouped inside a paranthesis
 * @author Jose
 *
 */
public class Parenthesized extends ASTNode{

  private final ASTNode expr;

  public Parenthesized(int line, int column, ASTNode expr) {
    super(line, column);
    this.expr = expr;
  }

  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public ASTNode getExpr() {
    return expr;
  }

  @Override
  public String toString() {
    return "PAREN ~ "+expr;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
