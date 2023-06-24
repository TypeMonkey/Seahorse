package jg.sh.parsing.nodes;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;
import jg.sh.parsing.token.Token;

/**
 * An identifier used to uniquely identify a Seahorse component: variables, data types, functions.
 * 
 * A valide Seahorse identifier must:
 * -> Begin with a letter [a-zA-Z], '$' or '_'
 * -> Subsequently be composed of alphanumerical symbols, as well as '$' and '_'
 */
public class Identifier extends Node {

  private final String identifier;

  public Identifier(Token tokenIden) {
    this(tokenIden.getContent(), tokenIden.getStart(), tokenIden.getEnd());
  }

  public Identifier(String identifier, Location start, Location end) {
    super(start, end);
    this.identifier = identifier;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Identifier && ((Identifier) obj).identifier.equals(identifier);
  }

  @Override
  public int hashCode() {
    return identifier.hashCode();
  }

  public String getIdentifier() {
    return identifier;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitIdentifier(parentContext, this);
  }

  @Override
  public String repr() {
    return identifier;
  }

  @Override
  public boolean isLValue() {
    return true;
  }
  
}
