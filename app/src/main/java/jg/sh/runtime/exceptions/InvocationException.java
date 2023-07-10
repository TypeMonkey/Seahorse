package jg.sh.runtime.exceptions;

import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.callable.Callable;

public class InvocationException extends Exception {
  
  private final Callable target;
  private final RuntimeError errorObject;

  public InvocationException(String message, Callable target) {
    super(message);
    this.target = target;
    this.errorObject = null;
  }
  
  public InvocationException(RuntimeError error, Callable target) {
    super(error.getAttr("msg").toString());
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
