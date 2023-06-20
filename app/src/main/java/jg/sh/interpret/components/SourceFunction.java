package jg.sh.interpret.components;

import java.util.List;
import java.util.Map;

import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.interpret.Interpreter;

public class SourceFunction implements Callable {
  
  private final List<Statement> statements;
  private final Map<String, Integer> freeVars;
  
  public SourceFunction(Map<String, Integer> freeVars, List<Statement> statements) {
    this.freeVars = freeVars;
    this.statements = statements;
  }

  @Override
  public int invoke(Interpreter interpreter) {
    return interpreter.executeFunction(statements);
  }

  @Override
  public boolean isCallable() {
    return false;
  }

}
