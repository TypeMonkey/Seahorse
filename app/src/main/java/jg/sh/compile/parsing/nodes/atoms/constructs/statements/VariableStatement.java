package jg.sh.compile.parsing.nodes.atoms.constructs.statements;

import jg.sh.common.presenters.VariablePresenter;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.ReservedWords;
import jg.sh.compile.parsing.nodes.atoms.Keyword;

/**
 * Represents a variable declaration statement.
 * @author Jose
 */
public class VariableStatement extends Statement{

  private final String name;
  private final ASTNode value;
  private final VariablePresenter presenter;

  public VariableStatement(int line, int column, String name, ASTNode value, Keyword ... modifiers) {
    super(line, column, modifiers);
    this.name = name;
    this.value = value;
    this.presenter = new VariablePresenter(name, Keyword.toReservedWords(modifiers));
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof VariableStatement) {
      VariableStatement other = (VariableStatement) obj;
      return other.getName().equals(name);
    }
    return false;
  }
  
  public ASTNode getAssgnedExpr() {
    return value;
  }

  public String getName() {
    return name;
  }
  
  public VariablePresenter getPresenter() {
    return presenter;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    return "VAR_DEC ~ ["+getModifiers()+"] , NAME: "+name+" := "+value;
  }

  public Object repr() {
    return null;
  }
}
