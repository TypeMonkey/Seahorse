package jg.sh.runtime.objects.callable;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeInstance;

public class RuntimeInternalCallable extends Callable {
  
  private final InternalFunction function;
  
  public RuntimeInternalCallable(RuntimeModule hostModule, RuntimeInstance self, InternalFunction function) {
    super(function.getSignature(), hostModule, self);
    this.function = function;
  }
  
  public InternalFunction getFunction() {
    return function;
  }
  
  @Override
  public Callable rebind(RuntimeInstance newSelf, HeapAllocator allocator) {
    return new RuntimeInternalCallable(getHostModule(), newSelf, function);
  }
  
  @Override
  protected void markAdditional(Cleaner cleaner) {}

}
