package jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks;

import java.util.List;
import java.util.stream.Collectors;

import jg.sh.compile_old.parsing.nodes.ASTNode;
import jg.sh.compile_old.parsing.nodes.NodeVisitor;
import jg.sh.compile_old.parsing.nodes.atoms.Keyword;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.Statement;

public class IfElse extends Block{
  
  private final ASTNode cond;
  private final List<IfElse> otherBranches;
  
  public IfElse(Keyword ifKeyword, ASTNode cond, List<Statement> branchCode, List<IfElse> otherBranches) {
    super(ifKeyword.getLine(), ifKeyword.getColumn(), ifKeyword, branchCode);
    this.cond = cond;
    this.otherBranches = otherBranches;
  }
  
  public List<Statement> getBranchCode() {
    return getStatements();
  }
  
  public ASTNode getCond() {
    return cond;
  }
  
  public List<IfElse> getOtherBranches() {
    return otherBranches;
  }
  
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    String x = "~"+getSignifier()+" , COND? "+(cond == null ? "" : cond.toString())+System.lineSeparator();
    
    x += getStatements().stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(System.lineSeparator()));
    
    x += otherBranches.stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(System.lineSeparator()));
    
    return x;
  }
}
