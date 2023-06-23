package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents an attribute access on an object: expr.attrName
 */
public class AttrAccess extends Node {

  private final Node target;
  private final Identifier attrName;

  public AttrAccess(Node target, Identifier attrName, Location start, Location end) {
    super(start, end);
    this.target = target;
    this.attrName = attrName;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitAttrAccess(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr()+"."+attrName.repr();
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
  public Node getTarget() {
    return target;
  }

  public Identifier getAttrName() {
    return attrName;
  }
}
