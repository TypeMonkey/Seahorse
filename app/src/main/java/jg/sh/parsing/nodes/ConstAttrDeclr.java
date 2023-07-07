package jg.sh.parsing.nodes;

import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * Represents an object attribute that's made to be immutable.
 * 
 * Format:
 * 
 * const obj(.attr)+ := value;
 * 
 * The attribute that being made immutable must be a new attribute, 
 * so instances of sealed data definiions cannot have 
 * const attribute outside their constructors.
 * 
 * Similarly, sealed objects cannot have const attributes appeneded to them.
 */
public class ConstAttrDeclr extends Node {

  private final AttrAccess attr;
  private final Node value;

  public ConstAttrDeclr(AttrAccess attr, Node value) {
    super(attr.start, value.end);
    this.attr = attr;
    this.value = value;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitConstAttrDeclr(parentContext, this);
  }

  public AttrAccess getAttr() {
    return attr;
  }

  public Node getValue() {
    return value;
  }

  @Override
  public String repr() {
    return "const "+attr.repr()+" := "+value.repr();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
