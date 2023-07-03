package jg.sh.parsing.nodes;

import java.util.Map;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * Represents an object literal.
 * 
 * Format:
 * 
 * object [sealed] {
 *   [const] attr1 : value1,
     ....
 * }
 */
public class ObjectLiteral extends Node {

  private final Map<String, Parameter> attributes;
  private final boolean isSealed;

  public ObjectLiteral(Map<String, Parameter> attributes, Location start, Location end, boolean isSealed) {
    super(start, end);
    this.attributes = attributes;
    this.isSealed = isSealed;
  }

  public Map<String, Parameter> getAttributes() {
    return attributes;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitObjectLiteral(parentContext, this);
  }

  @Override
  public String repr() {
    throw new UnsupportedOperationException("Unimplemented method 'repr'");
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public boolean isSealed() {
    return isSealed;
  }
}
