package jg.sh.compile.parsing.nodes.atoms.constructs.blocks;

import java.util.List;

import jg.sh.compile.parsing.nodes.NodeVisitor;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;

public class ScopeBlock extends Block{

  public ScopeBlock(int line, int column, Keyword curlyBrace, List<Statement> statements) {
    super(line, column, curlyBrace, statements);
    
  }

  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }

}
