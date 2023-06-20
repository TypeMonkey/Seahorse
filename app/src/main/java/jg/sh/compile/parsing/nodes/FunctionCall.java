package jg.sh.compile.parsing.nodes;

import java.util.Arrays;

public class FunctionCall extends ASTNode{
  
  private final ASTNode target;
  private final CallArg [] arguments;
  
  public FunctionCall(int line, int column, ASTNode targetFunction, CallArg [] arguments) {
    super(line, column);
    this.target = targetFunction;
    this.arguments = arguments;
  }
  
  public CallArg [] getArguments() {
    return arguments;
  }

  public int getArgCount() {
    return arguments.length;
  }

  public ASTNode getTarget() {
    return target;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "FCALL ~ ("+target+") with "+Arrays.toString(arguments);
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
