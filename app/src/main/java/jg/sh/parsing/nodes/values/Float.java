package jg.sh.parsing.nodes.values;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

public class Float extends Value<Double> {

  public Float(Double value, Location start, Location end) {
    super(value, start, end);
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitFloat(parentContext, this);
  }
  
}
