package jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks;

import java.util.List;
import java.util.stream.Collectors;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;
import jg.sh.compile_old.parsing.nodes.atoms.Keyword;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.Statement;

public class WhileLoop extends Block{
  
  private final ASTNode condition;
  private final List<Statement> statements;

  public WhileLoop(Keyword whileKeyword, ASTNode condition, List<Statement> body) {
    super(whileKeyword.getLine(), whileKeyword.getColumn(), whileKeyword, body);
    this.condition = condition;
    this.statements = body;
  }
  
  public ASTNode getCondition() {
    return condition;
  }
  
  public List<Statement> getStatements() {
    return statements;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    String x = "~WHILE , COND: "+condition+System.lineSeparator();
    
    x += statements.stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(System.lineSeparator()));
    
    return x;
  }

}
