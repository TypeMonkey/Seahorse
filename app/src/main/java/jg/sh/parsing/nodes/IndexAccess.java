package jg.sh.parsing.nodes;

import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * Access an object at a certain index value.
 * 
 * Format:
 *    expr[ indexExpr ]
 */
public class IndexAccess extends Node {

  private final Node target;
  private final Node index;

  public IndexAccess(Node target, Node index) {
    super(target.start, index.end);
    this.target = target;
    this.index = index;
  }

  public Node getIndex() {
    return index;
  }

  public Node getTarget() {
    return target;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitIndexAccess(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr() + "[ " + index.repr()+" ]";
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
}
