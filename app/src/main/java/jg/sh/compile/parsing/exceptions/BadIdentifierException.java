package jg.sh.compile.parsing.exceptions;

import jg.sh.compile.parsing.nodes.atoms.Identifier;

public class BadIdentifierException extends FormationException{

  public BadIdentifierException(Identifier badIdentifier, String fileName) {
    super(fileName, 
        "Cannot use '"+badIdentifier.getIdentifier()+"' as an identifier", 
        badIdentifier.getLine(), 
        badIdentifier.getColumn());
  }

  public BadIdentifierException(String fileName) {
    super(fileName, 
        "Cannot use '"+fileName+"' as a module name", 
        0, 
        0);
  }
}
