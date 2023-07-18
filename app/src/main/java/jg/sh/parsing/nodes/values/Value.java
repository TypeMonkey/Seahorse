package jg.sh.parsing.nodes.values;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Node;

/**
 * Represents a literal value - values declared "as is".
 * 
 * Seahorse supports integers (as 64-bit integers), floats (as 64-bit floats), strings
 * booleans and the null value as literal values.
 * 
 * Example:
 * -> an integer declared as a literal value: 521
 * -> a string declared as a literal value: "hello world" or 'hello world'
 */
public abstract class Value<V> extends Node {

  protected final V value;

  public Value(V value, Location start, Location end) {
    super(start, end);
    this.value = value;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Value && ((Value<?>) obj).value.equals(value);
  }

  @Override
  public abstract <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext);

  @Override
  public String repr() {
    return value.toString();
  }

  public V getValue() {
    return value;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
