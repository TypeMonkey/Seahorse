package jg.sh.compile.optimization;

import jg.sh.parsing.nodes.Node;

public class ValueInfo {
  
  private final Node original;
  private Node calculatedValue;

  public ValueInfo(Node original) {
    this.original = original;
  }

  public void setValue(Node value) {
    this.calculatedValue = value;
  }

  public Node getOriginal() {
    return original;
  }

  public Node getValue() {
    return calculatedValue;
  }
}
