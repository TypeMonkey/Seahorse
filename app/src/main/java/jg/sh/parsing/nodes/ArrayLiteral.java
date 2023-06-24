package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.stream.Collectors;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

/**
 * Represents an array literal.
 * 
 * Format:
 *   [value1, value2, ...]
 *   
 *   or just [] 
 */
public class ArrayLiteral extends Node {

  private final Node [] values;

  public ArrayLiteral(Location start, Location end, Node ... values) {
    super(start, end);
    this.values = values;
  }

  public Node[] getValues() {
    return values;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitArray(parentContext, this);
  }

  @Override
  public String repr() {
    return Arrays.asList(values).stream().map(Node::repr).collect(Collectors.joining(","));
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
