package jg.sh.parsing.nodes.values;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

public class Null extends Value<Void> {

  public Null(Location start, Location end) {
    super(null, start, end);
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitNull(parentContext, this);
  }
  
}
