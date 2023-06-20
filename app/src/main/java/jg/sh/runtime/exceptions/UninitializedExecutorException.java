package jg.sh.runtime.exceptions;

public class UninitializedExecutorException extends Exception {

  public UninitializedExecutorException() {
    super("Executor hasn't been initialized");
  }
  
}
