package jg.sh.compile_old.parsing.nodes.atoms.constructs.statements;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import jg.sh.compile_old.parsing.nodes.NodeVisitor;

/**
 * Represents a "use" expression, to import modules.
 * @author Jose
 */
public class UseStatement extends Statement {
  
  private final String targetModule;
  private final Set<String> components;
  
  public UseStatement(int line, int column, String targetModule, String ... components) {
    super(line, column);
    this.targetModule = targetModule;
    this.components = new HashSet<>(Arrays.asList(components));
  } 
  
  public UseStatement(int line, int column, String targetModule) {
    this(line, column, targetModule, new String[0]);
  } 

  public String getTargetModule() {
    return targetModule;
  }
  
  public Set<String> getComponents() {
    return components;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "USE ~ target: "+targetModule+" , comps: "+components;
  }
}
