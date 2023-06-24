package jg.sh.parsing.nodes.statements.blocks;

import java.util.List;

import jg.sh.common.Location;
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

  public Node getCondition() {
    return condition;
  }
}
