package jg.sh.compile_old.parsing.nodes.atoms.constructs.statements;

import java.util.Set;

import jg.sh.compile_old.parsing.nodes.NodeVisitor;

public class CaptureStatement extends Statement{

  private final Set<VariableStatement> identifiers;
  
  public CaptureStatement(int line, int column, Set<VariableStatement> identifiers) {
    super(line, column);
    this.identifiers = identifiers;
  }

  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  public Set<VariableStatement> getIdentifiers() {
    return identifiers;
  }

  @Override
  public String toString() {
    return "CAPT ~ "+identifiers;
  }

}
