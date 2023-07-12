package jg.sh.runtime.exceptions;

import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.callable.Callable;

/**
 * This exception is thrown by a Callable, indicating either
 * a RuntimeError thrown directly from it, or passed down from a previous
 * call due to a lack of a exception handler.
 */
public class InvocationException extends Exception {
  
  private final Callable target;
  private final RuntimeError errorObject;

  public InvocationException(String message, Callable target) {
    super(message);
    this.target = target;
    this.errorObject = null;
  }

  public InvocationException(OperationException exception, Callable target) {
    this(exception.getMessage(), target);
  }
  
  public InvocationException(RuntimeError error, Callable target) {
    super(error.getMessage());
    this.errorObject = error;
    this.target = target;
  }
  
  public boolean isWrapper() {
    return errorObject != null;
  }
  
  public RuntimeError getErrorObject() {
    return errorObject;
  }
  
  public Callable getTarget() {
    return target;
  }
}
