package jg.sh.compile.validation.exceptions;

import java.util.Arrays;

import jg.sh.compile.CompilationException;
import jg.sh.compile.parsing.nodes.ReservedWords;
import jg.sh.compile.validation.Context;
import jg.sh.compile.validation.Context.ContextType;

public class BadKeywordPlacement extends CompilationException{

  public BadKeywordPlacement(ReservedWords keyword, ContextType properContext, String moduleName, int line, int col) {
    super("The '"+keyword+"' can only be used within "+properContext.toString().toLowerCase(), moduleName, line, col);
  }

  public BadKeywordPlacement(ReservedWords keyword, String moduleName, int line, int col, ContextType ... properContexts) {
    super("The '"+keyword+"' can only be used within "+Arrays.toString(properContexts), moduleName, line, col);
  }
}
