package jg.sh.intake.nodes.simple;

import java.util.Map;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a dictionary literal declaration:
 * 
 * dict {
 *    let key1 := expr1;
 *    ...
 * }
 */
public class DictLiteralExpr extends Node {

  private final Map<Identifier, Node> attrs;

  public DictLiteralExpr(Map<Identifier, Node> attrs, Location start, Location end) {
    super(start, end);
    this.attrs = attrs;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitDictionary(parentContext, this);
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
  
  public Map<Identifier, Node> getAttrs() {
    return attrs;
  }
}
