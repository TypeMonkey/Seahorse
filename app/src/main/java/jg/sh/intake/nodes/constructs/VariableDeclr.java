package jg.sh.intake.nodes.constructs;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.simple.Identifier;

public class VariableDeclr extends Node {

  private final Identifier name;
  private final boolean isConst;
  private final Node initialValue;

  public VariableDeclr(Identifier name, boolean isConst, Node initialValue, Location start, Location end) {
    super(start, end);
    this.name = name;
    this.isConst = isConst;
    this.initialValue = initialValue;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitVarDec(parentContext, this);
  }

  public Node getInitialValue() {
    return initialValue;
  }

  public Identifier getName() {
    return name;
  }

  public boolean isConst() {
    return isConst;
  }

  @Override
  public String repr() {
    return (isConst ? "const " : "var ") + name.getName() + " := " + initialValue.repr();
  }

  @Override
  public boolean isLValue() {
    return true;
  }  
}
