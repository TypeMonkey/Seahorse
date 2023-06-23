package jg.sh.intake.nodes.simple;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;

public class Parameter extends Node {

  private final String name;
  private final Node initValue;
  private final boolean isConst;

  public Parameter(String name, Node initialVal, boolean isConst, Location start, Location end) {
    super(start, end);
    this.name = name;
    this.initValue = initialVal;
    this.isConst = isConst;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Parameter) {
      return ((Parameter) obj).name.equals(name);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getName() {
    return name;
  }

  public Node getInitValue() {
    return initValue;
  }

  public boolean isConst() {
    return isConst;
  }

  public boolean hasValue() {
    return initValue != null;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitParameter(parentContext, this);
  }

  @Override
  public String repr() {
    return (isConst ? "const " : "") + name + (hasValue() ? " := "+initValue.repr() : "");
  }

  @Override
  public boolean isLValue() {
    return false;
  }

}
