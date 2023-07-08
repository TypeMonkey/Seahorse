package jg.sh.parsing.nodes.statements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;

/**
 * Declares a variable in the current scope.
 * 
 * Format:
 * 
 * var (varNam [:= expr],)+;
 * 
 * or 
 * 
 * const (varName := expr,)+;
 */
public class VarDeclr extends Statement {

  private final Identifier name;
  private final boolean isConst;
  private final Node initialValue;
  private final Set<Keyword> descriptors;

  public VarDeclr(Identifier name, boolean isConst, Node initialValue, Location start, Location end, Keyword ... descriptors) {
    this(name, isConst, initialValue, start, end, new HashSet<>(Arrays.asList(descriptors)));
  }

  public VarDeclr(Identifier name, boolean isConst, Node initialValue, Location start, Location end, Set<Keyword> descriptors) {
    super(start, end);
    this.name = name;
    this.isConst = isConst;
    this.initialValue = initialValue;
    this.descriptors = descriptors;
  }

  public boolean isConst() {
    return isConst;
  }

  public Identifier getName() {
    return name;
  }

  public Node getInitialValue() {
    return initialValue;
  }

  public boolean hasInitialValue(){ 
    return initialValue != null;
  }

  public Set<Keyword> getDescriptors() {
    return descriptors;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitVarDeclr(parentContext, this);
  }

  @Override
  public String repr() {
    return name.getIdentifier() + (hasInitialValue() ? " := " + initialValue.repr() : "");
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
}
