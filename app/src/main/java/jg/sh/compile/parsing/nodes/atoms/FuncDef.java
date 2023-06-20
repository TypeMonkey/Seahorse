package jg.sh.compile.parsing.nodes.atoms;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.VariableStatement;

public class FuncDef extends ASTNode {
  
  private final String boundName;
  private final FunctionSignature signature;
  private final LinkedHashSet<VariableStatement> captures;
  private final LinkedHashMap<String, Parameter> allParams;
  private final List<Statement> statements;
    
  public FuncDef(int line, 
                 int column, 
                 String boundName,
                 FunctionSignature signature, 
                 LinkedHashSet<VariableStatement> captures,
                 LinkedHashMap<String, Parameter> allParams,
                 List<Statement> statements) {
    super(line, column);
    this.boundName = boundName;
    this.statements = statements;
    this.captures = captures;
    this.allParams = allParams;
    this.signature = signature;
  }
  
  public List<Statement> getStatements() {
    return statements;
  }
  
  public FunctionSignature getSignature() {
    return signature;
  }
  
  public LinkedHashMap<String, Parameter> getParams() {
    return allParams;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  public LinkedHashSet<VariableStatement> getCaptures() {
    return captures;
  }
  
  public String getBoundName() {
    return boundName;
  }
  
  public boolean hasName() {
    return boundName != null;
  }
  
  @Override
  public String toString() {
    String x = "~FUNC "+getBoundName()+System.lineSeparator();
    x += "  * SIGNATURE: "+ allParams + System.lineSeparator();
    x += "     "+getStatements()
                   .stream()
                   .map(s -> s.toString())
                   .collect(Collectors.joining(System.lineSeparator()));
    return x;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
