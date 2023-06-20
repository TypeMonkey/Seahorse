package jg.sh.compile.validation.exceptions;

import jg.sh.compile.CompilationException;
import net.percederberg.grammatica.parser.Token;

public class InvalidReassignmentException extends CompilationException{

  public InvalidReassignmentException(String identifier, String moduleName, int line, int column) {
    super("Cannot reaasign const variable '"+identifier+"'", 
             moduleName, 
             line, 
             column);
  }

}
