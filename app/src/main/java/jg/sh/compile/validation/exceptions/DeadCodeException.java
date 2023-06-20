package jg.sh.compile.validation.exceptions;

import jg.sh.compile.CompilationException;
import jg.sh.compile.parsing.nodes.ASTNode;

public class DeadCodeException extends CompilationException{

  public DeadCodeException(String moduleName, int line, int col) {
    super("The following code after this line will not be executed", moduleName, line, col);
  }

}
