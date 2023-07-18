package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jg.sh.parsing.token.TokenType;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * A paramter are the recievers of a value
 * (either by it's position or explicit passing by name) during
 * a function call.
 * 
 * Format:
 * 
   * [const] [var] [!] paramName [:= node]
 * 
 * where node is the initial value of the parameter.
 * 
 * Note: a varying/"var" parameter packs any remaining positional arguments into an
 *       array and passes that to the callee function. If "const var" parameter is still
 *       varying, but the parameter variable just can't be reassigned.
 */
public class Parameter extends Node {

  private final Identifier name;
  private final Node initValue;
  private final Set<Keyword> descriptors;
  private final boolean isVarArgsKeyword;

  public Parameter(Identifier name, Node initValue, boolean isVarArgsKeyword, Keyword ... descriptors) {
    this(name, initValue, isVarArgsKeyword, new HashSet<>(Arrays.asList(descriptors)));
  }

  public Parameter(Identifier name, Node initValue, boolean isVarArgsKeyword, Set<Keyword> descriptors) {
    super(name.start, name.end);
    this.name = name;
    this.initValue = initValue;
    this.descriptors = descriptors;
    this.isVarArgsKeyword = isVarArgsKeyword;
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

  public boolean isConst() {
    return Keyword.hasKeyword(TokenType.CONST, descriptors);
  }

  public boolean isVarying() {
    //This parameter takes an indefinite list of arguments.
    return Keyword.hasKeyword(TokenType.VAR, descriptors);
  }

  public boolean isVarArgsKeyword() {
    return isVarArgsKeyword;
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
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitParameter(parentContext, this);
  }

  @Override
  public String repr() {
    return (isConst() ? "const " : "") + 
           (isVarying() ? "var " : "") + 
           name.getIdentifier() + 
           (hasValue() ? initValue.repr() : "");
  }

  @Override
  public boolean isLValue() {
    return hasDescriptor(TokenType.CONST);
  }
  
}
