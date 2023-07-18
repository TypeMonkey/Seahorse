package jg.sh.runtime.objects.callable;

import jg.sh.runtime.objects.RuntimeInstance;

/**
 * This is a looser version of StrictFuncInterface for internal functions
 * that don't have any type expectations of their "self" object.
 * 
 * The self object can be any RuntimeInstance.
 */
@FunctionalInterface
public interface PervasiveFuncInterface extends StrictFuncInterface<RuntimeInstance> {
  
}
