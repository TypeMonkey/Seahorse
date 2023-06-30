package jg.sh.compile_old.parsing.nodes.atoms.constructs;

import java.util.List;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.UseStatement;

/**
 * Represents a grammatically sound SeaHorse module.
 * @author Jose
 */
public class Module extends ASTNode{
    
  private final String name;
  private final List<UseStatement> imports;
  private final List<Statement> statements;
    
  public Module(String name, List<UseStatement> imports, List<Statement> statements) {
    super(0, 0);
    this.name = name;
    this.imports = imports;
    this.statements = statements;
  }
  
  public List<Statement> getStatements() {
    return statements;
  }
  
  public List<UseStatement> getImports() {
    return imports;
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    String info = "~FILE: "+name+System.lineSeparator();
    
    info += "  --> IMPORTS "+System.lineSeparator();
    for (UseStatement useExpression : imports) {
      info += "     => "+useExpression+System.lineSeparator();
    }
    
    for(Statement statement : statements) {
      info += " * "+statement+System.lineSeparator();
    }
    
    return info;
  }

  @Override
  public boolean isLValue() {
    return false;
  }

}
