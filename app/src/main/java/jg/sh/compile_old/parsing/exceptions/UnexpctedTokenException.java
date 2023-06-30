package jg.sh.compile_old.parsing.exceptions;

import net.percederberg.grammatica.parser.Token;

public class UnexpctedTokenException extends FormationException {

  public UnexpctedTokenException(String fileName, Token token) {
    super(fileName, "Unexpected token '"+token.getImage()+"'", token.getStartLine(), token.getStartColumn());
  }
  
}
