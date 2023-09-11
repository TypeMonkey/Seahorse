package jg.sh.runtime.objects.callable;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeInstance;

public abstract class Callable extends RuntimeInstance {
  
  private final RuntimeModule hostModule;
  private final RuntimeInstance self;
  private final FunctionSignature signature;
  
  public Callable(FunctionSignature signature, RuntimeModule hostModule, RuntimeInstance self) {
    this.hostModule = hostModule;
    this.self = self;
    this.signature = signature;
  }

  @Override
  public Callable $call() {
    return this;
  }
  
  public RuntimeModule getHostModule() {
    return hostModule;
  }
  
  public RuntimeInstance getSelf() {
    return self;
  }
  
  public FunctionSignature getSignature() {
    return signature;
  }
  
  public abstract Callable rebind(RuntimeInstance target, HeapAllocator allocator);

  public String toString(){
      return getSelf().getClass()+" | "+getSelf();
  }
  
  @Override
  public void finalize(){
     
  }
}
