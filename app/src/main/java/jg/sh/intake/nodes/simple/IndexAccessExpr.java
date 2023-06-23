package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents an indexation operation: expr[arg]
 * 
 * Note: it's not necessary for "expr" to be an array. It just has to support indexation
 */
public class IndexAccessExpr extends Node {

  private final Node target;
  private final Node indexExpr;

  public IndexAccessExpr(Node target, Node indexExpr, Location start, Location end) {
    super(start, end);
    this.target = target;
    this.indexExpr = indexExpr;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitIndexAccess(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr()+"["+indexExpr.repr()+"]";
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
  public Node getTarget() {
    return target;
  }

  public Node getIndexExpr() {
    return indexExpr;
  }
}
