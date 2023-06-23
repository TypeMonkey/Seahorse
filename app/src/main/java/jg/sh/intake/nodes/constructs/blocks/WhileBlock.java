package jg.sh.intake.nodes.constructs.blocks;

import java.util.List;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.constructs.Statement;

public class WhileBlock extends BlockExpr {

    private final Node conditional;

    public WhileBlock(Node conditional, List<Statement> statements, Location start, Location end) {
        super(statements, start, end);
        this.conditional = conditional;
    }

    @Override
    public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
        return visitor.visitWhileBlock(parentContext, this);
    }
    
    public Node getCondtional() {
        return conditional;
    }

    @Override
    public String repr() {
        return "while "+conditional.repr() + super.repr();
    }
}
