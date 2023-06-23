package jg.sh.intake.nodes.simple;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.token.Token;

public class Identifier extends Node {

  private final String name;

  public Identifier(Token idenToken) {
    this(idenToken.getContent(), 
         new Location(idenToken.getLineNumber(), idenToken.getStartCol()), 
         new Location(idenToken.getLineNumber(), idenToken.getEndCol()));
  }

  public Identifier(String name, Location start, Location end) {
    super(start, end);
    this.name = name;
  }

  @Override
  public boolean equals(Object arg0) {
    return arg0 instanceof Identifier ? ((Identifier) arg0).name.equals(name) : false;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
    return visitor.visitIdentifier(parentContext, this);
  }

  @Override
  public String repr() {
    return name;
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
  public String getName() {
    return name;
  }
}
