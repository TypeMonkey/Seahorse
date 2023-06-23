package jg.sh.intake.nodes.values;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a decimal number (0, negative and positive)
 */
public class Float extends Node {

  private final double value;

  public Float(double value, Location start, Location end) {
    super(start, end);
    this.value = value;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitFloat(parentContext, this);
  }

  @Override
  public String repr() {
    return String.valueOf(value);
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public double getValue() {
    return value;
  }
}
