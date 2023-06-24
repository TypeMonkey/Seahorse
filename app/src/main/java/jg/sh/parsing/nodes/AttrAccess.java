package jg.sh.parsing.nodes;

import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

/**
 * Represents the accessing of an object's attribute
 * 
 * Format:
 * 
 * expr.attr
 * 
 * where attr is an identifier
 */
public class AttrAccess extends Node {

  private final Node target;
  private final Identifier attrName;

  public AttrAccess(Node target, Identifier attrName) {
    super(target.start, attrName.end);
    this.target = target;
    this.attrName = attrName;
  }

  public Node getTarget() {
    return target;
  }

  public Identifier getAttrName() {
    return attrName;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitAttrAccess(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr() + "." + attrName.repr();
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
}
