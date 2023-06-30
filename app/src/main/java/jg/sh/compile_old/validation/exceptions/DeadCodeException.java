package jg.sh.compile_old.validation.exceptions;

import jg.sh.compile_old.CompilationException;
import jg.sh.compile_old.parsing.nodes.ASTNode;

public class DeadCodeException extends CompilationException{

  public DeadCodeException(String moduleName, int line, int col) {
    super("The following code after this line will not be executed", moduleName, line, col);
  }

}
