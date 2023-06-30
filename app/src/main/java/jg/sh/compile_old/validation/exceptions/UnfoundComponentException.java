package jg.sh.compile_old.validation.exceptions;

import jg.sh.compile_old.CompilationException;
import jg.sh.compile_old.parsing.nodes.atoms.Identifier;
import net.percederberg.grammatica.parser.Token;

public class UnfoundComponentException extends CompilationException{
  
  public UnfoundComponentException(String identifier, String moduleName, int line, int col) {
    super("Unknown component with name '"+identifier+"'", 
        moduleName, 
        line, 
        col);
  }
}
