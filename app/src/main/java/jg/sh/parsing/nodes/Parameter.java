package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jg.sh.parsing.token.TokenType;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;

/**
 * A paramter are the recievers of a value
 * (either by it's position or explicit passing by name) during
 * a function call.
 * 
 * Format:
 * 
 * (const | var) paramName [:= node]
 * 
 * where node is the initial value of the parameter
 */
public class Parameter extends Node {

  private final Identifier name;
  private final Node initValue;
  private final Set<Keyword> descriptors;

  public Parameter(Identifier name, Node initValue, Keyword ... descriptors) {
    super(name.start, name.end);
    this.name = name;
    this.initValue = initValue;
    this.descriptors = new HashSet<>(Arrays.asList(descriptors));
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Parameter && ((Parameter) obj).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public boolean hasValue() {
    return initValue != null;
  }

  public Identifier getName() {
    return name;
  }

  public Node getInitValue() {
    return initValue;
  }

  public Set<Keyword> getDescriptors() {
    return descriptors;
  }

  public boolean hasDescriptor(Keyword keyword) {
    return descriptors.contains(keyword);
  }

  public boolean hasDescriptor(TokenType keyword) {
    return descriptors.stream().anyMatch(x -> x.getKeyword() == keyword);
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitParameter(parentContext, this);
  }

  @Override
  public String repr() {
    throw new UnsupportedOperationException("Unimplemented method 'repr'");
  }

  @Override
  public boolean isLValue() {
    return hasDescriptor(TokenType.CONST);
  }
  
}
