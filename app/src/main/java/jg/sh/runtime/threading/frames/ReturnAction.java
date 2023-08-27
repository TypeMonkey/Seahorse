package jg.sh.runtime.threading.frames;

import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;

@FunctionalInterface
public interface ReturnAction {
  
  /**
   * Called when a StackFrame returns a value,
   * or throws an exception to the callee
   * @param returnValue - the RuntimeInstance returned by the StackFrame
   * @param errorValue - the RuntimeError throws by the StackFrame
   * 
   * Note: returnValue and errorValue are null in a mutuall-exclusive sense.
   *       Meaning if a StackFrame returns a value (note: null is represented as RuntimeNull)
   *       , then errorValue is guarenteed to be actually null (i.e: errorValue == null), and
   *       and vice-versa
   */
  public void ret(RuntimeInstance returnValue, RuntimeError errorValue);

}
