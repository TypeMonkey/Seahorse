package jg.sh.parsing.nodes.statements;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;

public class ReturnStatement extends Statement {

  public ReturnStatement(Keyword returnKeyword, Location end) {
    this(returnKeyword, null, end);
  }

  public ReturnStatement(Keyword returnKeyword, Node value, Location end) {
    super(value, returnKeyword.start, end);
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitReturnStatement(parentContext, this);
  }

  @Override
  public String repr() {
    return "RETURN "+getValue().repr()+";";
  }

  public boolean hasValue() {
    return getExpr() != null;
  }

  public Node getValue() {
    return getExpr();
  }
  
}
