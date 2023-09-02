package jg.sh.runtime.threading.stackops;

import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.threading.frames.StackFrame;

@FunctionalInterface
public interface ErrorContinuer {
  
  public StackFrame error(RuntimeInstance topInstance, RuntimeInstance secInstance, OperationException err);

}
