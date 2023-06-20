package jg.sh.runtime.exceptions;

import jg.sh.runtime.objects.RuntimeError;

/**
 * Exception to be caught by the system handler.
 * 
 * The system handler is the final catcher of all exceptions that
 * wasn't handled on user space
 */
public class SystemHandlerException extends Exception{

  private final RuntimeError exception;
  
  public SystemHandlerException(RuntimeError exception) {
    super(exception.getAttr("msg").toString());
    this.exception = exception;
  }
  
  public RuntimeError getException() {
    return exception;
  }
}
