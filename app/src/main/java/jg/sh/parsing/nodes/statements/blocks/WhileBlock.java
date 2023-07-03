package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.statements.Statement;

public class WhileBlock extends Block {

  private final Node condition;

  public WhileBlock(Node condition, 
                    List<Statement> statements, 
                    Location start,
                    Location end) {
    super(statements, start, end);
    this.condition = condition;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitWhileBlock(parentContext, this);
  }

  public Node getCondition() {
    return condition;
  }
}
