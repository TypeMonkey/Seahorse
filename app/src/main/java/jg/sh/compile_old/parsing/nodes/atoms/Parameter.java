package jg.sh.compile_old.parsing.nodes.atoms;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class Parameter extends ASTNode{
  
  private final String name;
  private final ASTNode initValue;
  private final boolean isConst;
  private final boolean isVariableParam;

  public Parameter(int line, int column, String name, boolean isConst, boolean isVariableParam) {
    this(line, column, name, null, isConst, isVariableParam);
  }
  
  public Parameter(int line, int column, String name, ASTNode initValue, boolean isConst, boolean isVariableParam) {
    super(line, column);
    this.name = name;
    this.initValue = initValue;
    this.isConst = isConst;
    this.isVariableParam = isVariableParam;
  }

  public String getName() {
    return name;
  }
  
  public ASTNode getInitValue() {
    return initValue;
  }
  
  public boolean isConst() {
    return isConst;
  }
  
  public boolean isAKeywordParameter() {
    return initValue != null;
  }
  
  public boolean isVariableParam() {
    return isVariableParam;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "PARAM ~ "+name+" || val? "+(isAKeywordParameter() ? initValue.toString() : "");
  }

  @Override
  public boolean isLValue() {
    return true;
  }
}
