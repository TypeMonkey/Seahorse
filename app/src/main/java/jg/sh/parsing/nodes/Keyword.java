package jg.sh.parsing.nodes;

import jg.sh.parsing.token.TokenType;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;
import jg.sh.parsing.token.Token;

/**
 * Represents a declarative keyword.
 * 
 * A declarative keyword declares/describes a certain property
 * about a Node on the syntax tree.
 * 
 * Example declarative keywords and use cases:
 * * const => used in data constructors to describe which attributes are constant
 * * sealed => used on data type definitions to declare that said data type 
 *              cannot have attributes added on them after instantiation
 * * export => used on top-level components to allow import by other modules
 * * break => used to break out of a loop it's contained in
 * * continue => used to continue the loop it's contained in
 * * throw => the expression that comes after it is throw as error object
 */
public class Keyword extends Node {

  private final TokenType keyword;

  public Keyword(TokenType keyword, Location start, Location end) {
    super(start, end);
    this.keyword = keyword;
  }
  
  public Keyword(Token keywordToken) {
    super(keywordToken.getStart(), keywordToken.getEnd());
    this.keyword = keywordToken.getType();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Keyword && ((Keyword) obj).keyword == keyword;
  }

  @Override
  public int hashCode() {
    return keyword.hashCode();
  }

  public TokenType getKeyword() {
    return keyword;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitKeyword(parentContext, this);
  }

  @Override
  public String repr() {
    return keyword.name();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
  public static boolean hasKeyword(TokenType keyword, Collection<Keyword> keywords) {
    return keywords.stream().anyMatch(x -> x.getKeyword() == keyword);
  }

  public static boolean hasKeyword(TokenType keyword, Keyword ... keywords) {
    return hasKeyword(keyword, new HashSet<>(Arrays.asList(keywords)));
  }
}
