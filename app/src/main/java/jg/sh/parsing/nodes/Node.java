package jg.sh.parsing.nodes;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

/**
 * Root type for all nodes in a syntax tree.
 */
public abstract class Node {
  public final Location start;
  public final Location end;

  public Node(Location start, Location end) {
    this.start = start;
    this.end = end;
  }

  public abstract <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext);

  public abstract String repr();

  public abstract boolean isLValue();

  public String toString() {
    return repr() + " " + start + " <-> " + end;
  }
}
