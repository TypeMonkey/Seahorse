package jg.sh.intake.nodes;

import jg.sh.intake.Location;

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
