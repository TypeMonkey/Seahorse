package jg.sh.parsing.nodes.values;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

public class Null extends Value<Void> {

  public Null(Location start, Location end) {
    super(null, start, end);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Null;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitNull(parentContext, this);
  }
  
}
