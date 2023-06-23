package jg.sh.intake.nodes.values;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a whole number (0, negative and positive)
 */
public class Int extends Node{

  private final long value;

  public Int(long value, Location start, Location end) {
    super(start, end);
    this.value = value;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitInt(parentContext, this);
  }

  public long getValue() {
    return value;
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  @Override
  public String repr() {
    return String.valueOf(value);
  }
    
}
