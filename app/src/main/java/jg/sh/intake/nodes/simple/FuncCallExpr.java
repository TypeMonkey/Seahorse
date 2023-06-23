package jg.sh.intake.nodes.simple;

import java.util.Arrays;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents a function invocation: expr(arg1, arg2, ...)
 * where "expr" must be a function
 */
public class FuncCallExpr extends Node {

  private final Node target;
  private final Node [] args;

  public FuncCallExpr(Node target, Node [] args, Location start, Location end) {
    super(start, end);
    this.target = target;
    this.args = args;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitCall(parentContext, this);
  }

  @Override
  public String repr() {
    return target.repr()+"("+Arrays.stream(args)
                                   .map(Node::repr)
                                   .collect(Collectors.joining(","))+")";
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public Node getTarget() {
    return target;
  }

  public Node[] getArgs() {
    return args;
  }
}
