package jg.sh.runtime.threading.frames;

import jg.sh.modules.NativeModule;
import jg.sh.modules.NativeModuleDiscovery;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

public class JavaFrame extends StackFrame{

  private final ArgVector initialArgs;
  
  public JavaFrame(RuntimeModule hostModule, RuntimeInternalCallable callable, ArgVector initialArgs) {
    super(hostModule, callable);
    this.initialArgs = initialArgs;
  }
  
  @Override
  public StackFrame run(HeapAllocator allocator, Fiber thread) {
    final RuntimeInternalCallable internalCallable = getRuntimeInternalCallable();
    try {     
      RuntimeInstance returnValue = internalCallable.getFunction().invoke(thread, initialArgs);
      returnValue(returnValue);
    } catch (InvocationException e) {
      setErrorFlag(allocator.allocateError(e.getMessage()));
    }
    return null;
  }

  /**
   * Does nothing.
   */
  public void storeLocalVar(int varIndex, RuntimeInstance value) {
    return;
  }
  
  @Override
  protected void markAdditional(Cleaner cleaner) {
    cleaner.gcMarkObject(initialArgs);
  }
  
  /**
   * Convenience method for returning the RuntimeCallable that this FunctionFrame is based on.
   * @return the RuntimeCallable that this FunctionFrame is based on.
   */
  public RuntimeInternalCallable getRuntimeInternalCallable() {
    return (RuntimeInternalCallable) callable;
  }
  
  public ArgVector getInitialArgs() {
    return initialArgs;
  }
  
}
