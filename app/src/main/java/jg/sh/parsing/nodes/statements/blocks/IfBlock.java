package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.statements.Statement;

/**
 * Conditional control flow.
 * 
 * Format:
 * 
 * if (expr) {
 * 
 * }
 * (elif(expr) {} )*
 * [else {}]
 */
public class IfBlock extends Block {

  private final Keyword keyword;
  private final Node condition;
  private final List<IfBlock> otherBranches;

  public IfBlock(Keyword ifKeyword, 
                 Node condition, 
                 List<Statement> statements, 
                 List<IfBlock> otherBranches, 
                 Location end) {
    super(statements, ifKeyword.start, end);
    this.condition = condition;
    this.keyword = ifKeyword;
    this.otherBranches = otherBranches;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitIfBlock(parentContext, this);
  }
  
  public Keyword getKeyword() {
    return keyword;
  }

  public Node getCondition() {
    return condition;
  }

  public List<IfBlock> getOtherBranches() {
    return otherBranches;
  }
}
