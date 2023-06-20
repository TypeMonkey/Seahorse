package jg.sh.compile.validation.exceptions;

import jg.sh.compile.CompilationException;
import jg.sh.compile.parsing.nodes.atoms.Identifier;
import net.percederberg.grammatica.parser.Token;

public class UnfoundComponentException extends CompilationException{
  
  public UnfoundComponentException(String identifier, String moduleName, int line, int col) {
    super("Unknown component with name '"+identifier+"'", 
        moduleName, 
        line, 
        col);
  }
}
