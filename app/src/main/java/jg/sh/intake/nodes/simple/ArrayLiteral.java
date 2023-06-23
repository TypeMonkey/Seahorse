package jg.sh.intake.nodes.simple;

import java.util.List;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

/**
 * Represents an array literal declaration: [expr1 , expr2, .... ] or just []
 */
public class ArrayLiteral extends Node {

    private final List<Node> content;

    public ArrayLiteral(List<Node> content, Location start, Location end) {
        super(start, end);
        this.content = content;
    }

    public List<Node> getContent() {
        return content;
    }

    @Override
    public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
        return visitor.visitArrayLiteral(parentContext, this);
    }

    @Override
    public String repr() {
        return "["+content.stream().map(Node::repr).collect(Collectors.joining(","))+"]";
    }

    @Override
    public boolean isLValue() {
        return false;
    }
    
}
