package jg.sh.intake.nodes.values;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a sequence of characters, enclosed in a set of double quotes
 */
public class Str extends Node {

  private final String content;

  public Str(String content, Location start, Location end) {
    super(start, end);
    this.content = content;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitStr(parentContext, this);
  }

  @Override
  public String repr() {
    return content;
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  public String getValue() {
    return content;
  }
    
}
