package jg.sh.parsing.nodes;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.token.Token;

public class Operator extends Node {

  private final Op op; 

  public Operator(Token token) {
    super(token.getStart(), token.getEnd());
    this.op = Op.stringToOp.get(token.getContent());

    if (this.op == null) {
      throw new IllegalArgumentException("Unknown operator "+token);
    }
  }

  public Operator(Op op, Location start, Location end) {
    super(start, end);
    this.op = op;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof Operator && ((Operator) o).getOp() == op;
  }

  @Override
  public int hashCode() {
    return op.hashCode();
  }

  public Op getOp() {
    return op;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitOperator(parentContext, this);
  }

  @Override
  public String repr() {
    return op.str;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
