package jg.sh.runtime.threading.frames;

import java.util.function.BiConsumer;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

public class JavaFrame extends StackFrame{

  private final RuntimeInternalCallable callable;
  
  public JavaFrame(RuntimeModule hostModule, 
                   RuntimeInternalCallable callable, 
                   ArgVector initialArgs,
                   ReturnAction action,
                   Fiber fiber) {
    super(hostModule, initialArgs, action, fiber);
    this.callable = callable;
  }
  
  @Override
  public StackFrame run(HeapAllocator allocator) {
    final RuntimeInternalCallable internalCallable = getRuntimeInternalCallable();
    try {     
      RuntimeInstance returnValue = internalCallable.getFunction().invoke(fiber, initialArgs);
      returnValue(returnValue);
    } catch (InvocationException e) {
      returnError(allocator.allocateError(e.getMessage()));
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
  protected void markAdditional(Cleaner cleaner) {}
  
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
  
  public RuntimeInternalCallable getCallable() {
    return callable;
  }
}
