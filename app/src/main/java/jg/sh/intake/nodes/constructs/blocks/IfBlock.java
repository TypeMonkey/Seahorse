package jg.sh.intake.nodes.constructs.blocks;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.constructs.Statement;
import jg.sh.intake.token.TokenType;

public class IfBlock extends BlockExpr {

    private final TokenType keyword;
    private final Node conditional;
    private final List<IfBlock> alternates;

    public IfBlock(TokenType keyword, Node conditional, List<Statement> statements, Location start, Location end) {
        this(keyword, conditional, statements, Collections.emptyList(), start, end);
    }

    public IfBlock(TokenType keyword, Node conditional, List<Statement> statements, List<IfBlock> alternates, Location start, Location end) {
        super(statements, start, end);
        this.keyword = keyword;
        this.conditional = conditional;
        this.alternates = alternates;
    }

    @Override
    public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
        return visitor.visitIfBlock(parentContext, this);
    }

    public Node getConditional() {
        return conditional;
    }

    public List<IfBlock> getAlternates() {
        return alternates;
    }

    public TokenType getKeyword() {
        return keyword;
    }

    public String repr() {
        return keyword.name().toLowerCase()+" "+conditional.repr()+super.repr()
                      +(alternates.stream().map(Node::repr).collect(Collectors.joining()));
    }
}
