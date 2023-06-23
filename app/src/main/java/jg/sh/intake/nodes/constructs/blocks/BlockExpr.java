package jg.sh.intake.nodes.constructs.blocks;

import java.util.List;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.constructs.Statement;

public class BlockExpr extends Node {

    private final List<Statement> statements;

    public BlockExpr(List<Statement> statements, Location start, Location end) {
        super(start, end);
        this.statements = statements;
    }

    @Override
    public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
        return visitor.visitBlock(parentContext, this);
    }

    @Override
    public String repr() {
        return "{"+statements.stream().map(Node::repr).collect(Collectors.joining())+"}";
    }

    @Override
    public boolean isLValue() {
        return false;
    }
    
    public List<Statement> getStatements() {
        return statements;
    }
}
