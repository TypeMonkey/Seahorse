package jg.sh.parsing;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

  @Override
  public String toString() {
    String r = "==> Module: "+name+System.lineSeparator();

    r += " *** Imports: " + System.lineSeparator()
         + imports.stream().map(UseStatement::repr).collect(Collectors.joining(System.lineSeparator()));

    r += " *** Statements: " + System.lineSeparator()
         + statements.stream().map(Statement::repr).collect(Collectors.joining(System.lineSeparator()));

    return r;
  }
}
