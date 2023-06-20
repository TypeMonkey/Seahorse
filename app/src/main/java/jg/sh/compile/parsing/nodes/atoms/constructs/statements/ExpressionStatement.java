package jg.sh.compile.parsing.nodes.atoms.constructs.statements;

import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.ReservedWords;

public class ExpressionStatement extends Statement{

  private final ASTNode expr;
  private final ReservedWords leadingKeyword;
  
  public ExpressionStatement(int line, int column, ASTNode expr, ReservedWords leadingKeyword) {
    super(line, column);
    this.expr = expr;
    this.leadingKeyword = leadingKeyword;
  }

  public ASTNode getExpr() {
    return expr;
  }
  
  public ReservedWords getLeadingKeyword() {
    return leadingKeyword;
  }
  
  public boolean hasLeadingKeyword() {
    return leadingKeyword != null;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ESTATE ~ "+(leadingKeyword != null ? leadingKeyword.toString() : "")+" "+expr;
  }
}
