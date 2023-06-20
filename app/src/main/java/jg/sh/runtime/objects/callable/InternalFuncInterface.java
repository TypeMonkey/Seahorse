package jg.sh.runtime.objects.callable;

import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.threading.fiber.Fiber;

@FunctionalInterface
public interface InternalFuncInterface {

  public RuntimeInstance call(Fiber thread, ArgVector args) throws InvocationException;

}
