package jg.sh.parsing.nodes.statements;

import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Node;

/**
 * A list of variable declrations defined in sequence in one expression.
 * 
 * Format:
 * 
 * var [export] (varNam [:= expr],)+;
 * 
 * or 
 * 
 * const [export] (varName := expr,)+;
 */
public class VarDeclrList extends Statement {

  private final LinkedHashSet<VarDeclr> varDeclrs;

  public VarDeclrList(LinkedHashSet<VarDeclr> varDeclrs, Location start, Location end) {
    super(start, end);
    this.varDeclrs = varDeclrs;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitVarDeclrList(parentContext, this);
  }

  @Override
  public String repr() {
    return varDeclrs.stream().map(Node::repr).collect(Collectors.joining(System.lineSeparator()));
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public LinkedHashSet<VarDeclr> getVarDeclrs() {
    return varDeclrs;
  }
}
