package jg.sh.runtime.objects.callable;

import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.threading.fiber.Fiber;

/**
 * Represents an function that expects a specific type as
 * its "self" object.
 * 
 * Prior to invoking call(), proper checking and casting should be done
 * to ensure that the "self" parameter has been casted to the expected type, or else
 * the caller should throw an OperationException.
 */
@FunctionalInterface
public interface StrictFuncInterface<T extends RuntimeInstance> {

  public RuntimeInstance call(Fiber thread, T self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException;

}
