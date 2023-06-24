package jg.sh.intake.nodes.simple;

import java.util.Map;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.constructs.VariableDeclr;

/**
 * Represents a dictionary literal declaration:
 * 
 * dict [sealed] {
 *    (const | var) key1 := expr1;
 *    ...
 * }
 */
public class ObjectLiteralExpr extends Node {

  private final Map<Identifier, VariableDeclr> attrs;
  private final boolean isSealed;

  public ObjectLiteralExpr(Map<Identifier, VariableDeclr> attrs, boolean isSealed, Location start, Location end) {
    super(start, end);
    this.attrs = attrs;
    this.isSealed = isSealed;
  }

  public boolean isSealed() {
    return isSealed;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitObjectLiteral(parentContext, this);
  }

  @Override
  public String repr() {
    return "{"+attrs.entrySet()
                    .stream()
                    .map(x -> x.getKey()+" = "+x.getValue())
                    .collect(Collectors.joining(";"))+"}";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Map<Identifier, VariableDeclr> getAttrs() {
    return attrs;
  }
}
