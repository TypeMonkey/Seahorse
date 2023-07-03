package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.stream.Collectors;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;

/**
 * Represents a function call
 * 
 * Format:
 * 
 * <expr>([argument, ...])
 * 
 * where argument is of the format:
 *   argument := expr | name := expr
 * 
 * 
 */
public class FuncCall extends Node {

  public static class Argument {
    private final Identifier paramName;
    private final Node argument;

    public Argument(Node argument) {
      this(null, argument);
    }

    public Argument(Identifier paramName, Node argument) {
      this.paramName = paramName;
      this.argument = argument;
    }

    public Identifier getParamName() {
      return paramName;
    }

    public Node getArgument() {
      return argument;
    }

    public boolean hasName() {
      return paramName != null;
    }

    @Override
    public String toString() {
      return (paramName != null ? paramName.getIdentifier() + " := " : "") + argument;
    }
  }

  private final Node target;
  private final Argument [] arguments;

  public FuncCall(Node target, Location end, Argument ... args) {
    super(target.start, end);
    this.target = target;
    this.arguments = args;
  }

  public Node getTarget() {
    return target;
  }

  public Argument[] getArguments() {
    return arguments;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitCall(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr() + "(" + Arrays.asList(arguments).stream().map(Argument::toString).collect(Collectors.joining(",")) + ")";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
