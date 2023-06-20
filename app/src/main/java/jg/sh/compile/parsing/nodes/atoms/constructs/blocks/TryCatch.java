package jg.sh.compile.parsing.nodes.atoms.constructs.blocks;

import java.util.List;
import java.util.stream.Collectors;

import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.atoms.Identifier;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;

public class TryCatch extends Block{

  private final Identifier errorVar;
  private final List<Statement> handleBlock;
  
  public TryCatch(Keyword tryKeyword, List<Statement> targetBlock, Identifier errorVar, List<Statement> handleBlock) {
    super(tryKeyword.getLine(), tryKeyword.getColumn(), tryKeyword, targetBlock);
    this.errorVar = errorVar;
    this.handleBlock = handleBlock;
  }
  
  public Identifier getErrorVar() {
    return errorVar;
  }
  
  public List<Statement> getTargetBlock() {
    return getStatements();
  }
  
  public List<Statement> getHandleBlock() {
    return handleBlock;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public String toString() {
    String x = "~TRY_CATCH"+System.lineSeparator();
    
    x += getStatements().stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(System.lineSeparator()));
    
    x += " -> ERROR HANDLE: "+errorVar;
    
    x += handleBlock.stream()
        .map(s -> s.toString())
        .collect(Collectors.joining(System.lineSeparator()));
    
    return x;
  }

}
