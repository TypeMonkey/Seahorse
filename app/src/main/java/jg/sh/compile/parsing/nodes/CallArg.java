package jg.sh.compile.parsing.nodes;

public class CallArg extends ASTNode{
  
  private final String paramName;
  private final ASTNode argument;
  
  public CallArg(ASTNode argument) {
    this(null, argument);
  }
  
  public CallArg(String paramName, ASTNode argument) {
    super(argument.getLine(), argument.getColumn());
    this.paramName = paramName;
    this.argument = argument;
  }
  
  public boolean isKeywordArg() {
    return paramName != null;
  }
  
  public ASTNode getArgument() {
    return argument;
  }
  
  public String getParamName() {
    return paramName;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return paramName == null ? argument.toString() : paramName + " := "+argument;
  }

  @Override
  public boolean isLValue() {
    return argument.isLValue();
  }
}
