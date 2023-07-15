package jg.sh.runtime.exceptions;

import jg.sh.runtime.objects.callable.Callable;

/**
 * Indicates failure to create a StackFrame. This is generally
 * due to missing or over-supplied function arguments compared 
 * to the set parameters of the callee function.
 */
public class CallSiteException extends Exception {
  
  private final Callable target;

  public CallSiteException(String message, Callable target) {
    super(message);
    this.target = target;
  }
  
  public Callable getTarget() {
    return target;
  }
}
