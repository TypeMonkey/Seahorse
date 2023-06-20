package jg.sh.compile.parsing.exceptions;

import jg.sh.compile.CompilationException;

public class FormationException extends CompilationException{

  public FormationException(String fileName, String message, int line, int column) {
    super(message, fileName, column, column);
  }
  
}
