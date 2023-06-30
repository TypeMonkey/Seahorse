package jg.sh.compile_old;

public class CompilationException extends RuntimeException{

  public CompilationException(String message, String moduleName, int line, int col) {
    super(message+" at <ln: "+line+", col: "+col+"> , "+moduleName+".shr");
  }
}
