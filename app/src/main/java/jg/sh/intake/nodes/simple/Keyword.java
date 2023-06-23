package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.token.TokenType;

public class Keyword extends Node {

  private final TokenType keyword;

  public Keyword(TokenType keyword, Location start, Location end) {
    super(start, end);
    this.keyword = keyword;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitKeyword(parentContext, this);
  }

  @Override
  public String repr() {
    return keyword.name().toLowerCase();
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  public TokenType getKeyword() {
    return keyword;
  }
}
