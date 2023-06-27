package jg.sh.parsing;

import java.util.List;
import java.util.Map;

import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.UseStatement;

public class Module {
  
  private final String name;
  private final List<UseStatement> imports;
  private final List<Statement> statements;

  public Module(String name, List<UseStatement> imports, List<Statement> statements) {
    this.name = name;
    this.imports = imports;
    this.statements = statements;
  }

  public String getName() {
    return name;
  }

  public List<UseStatement> getImports() {
    return imports;
  }

  public List<Statement> getStatements() {
    return statements;
  }
}
