package jg.sh.runtime.objects.callable;

import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeInstance;

/**
 * Marks a Java-based method that can be immediately called
 * without making a seperate {@link JavaFrame} for it.
 * 
 * This annotation is meant for Seahorse native types and their 
 * Java-based functions - like toString() on RuntimeInstances.
 */
public class ImmediateInternalCallable extends RuntimeInternalCallable {

  public ImmediateInternalCallable(RuntimeModule hostModule, RuntimeInstance self, InternalFunction function) {
    super(hostModule, self, function);
  }
  
}
