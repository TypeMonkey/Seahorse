package jg.sh.compile.parsing.nodes.atoms.constructs.statements;

import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.ReservedWords;
import jg.sh.compile.parsing.nodes.atoms.Keyword;

public class KeywordStatement extends Statement{

  private final Keyword keyword;
  
  public KeywordStatement(int line, int column, Keyword keyword) {
    super(line, column);
    this.keyword = keyword;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  public Keyword getKeyword() {
    return keyword;
  }
  
  public ReservedWords getReservedWord() {
    return keyword.getKeyWord();
  }

  @Override
  public String toString() {
    return "KSTATE ~ "+keyword;
  }
}
