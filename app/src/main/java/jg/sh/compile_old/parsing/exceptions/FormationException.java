package jg.sh.compile_old.parsing.exceptions;

import jg.sh.compile_old.CompilationException;

public class FormationException extends CompilationException{

  public FormationException(String fileName, String message, int line, int column) {
    super(message, fileName, column, column);
  }
  
}
