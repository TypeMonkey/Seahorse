package jg.sh.intake.nodes.values;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a boolean literal value: true or false
 */
public class Bool extends Node {

  private final boolean value;

  public Bool(boolean value, Location start, Location end) {
    super(start, end);
    this.value = value;
  }

  public boolean getValue() {
    return value;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitBool(parentContext, this);
  }

  @Override
  public String repr() {
    return String.valueOf(value);
  }

  @Override
  public boolean isLValue() {
    return false;
  }
    
}
