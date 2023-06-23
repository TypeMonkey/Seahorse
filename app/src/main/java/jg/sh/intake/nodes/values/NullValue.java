package jg.sh.intake.nodes.values;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents the null value
 */
public class NullValue extends Node {

  public NullValue(Location start, Location end) {
    super(start, end);
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitNull(parentContext, this);
  }

  @Override
  public String repr() {
    return "null";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
    
}
