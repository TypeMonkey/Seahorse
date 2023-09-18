package jg.sh.runtime.objects;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;

public class RuntimeInternalDataRecord extends RuntimeDataRecord {

  private final InternalFunction constructor;
  private final Map<String, InternalFunction> methods;

  public RuntimeInternalDataRecord(String name, 
                                   InternalFunction constructor, 
                                   Map<String, InternalFunction> methods, 
                                   boolean instancesSealed) {
    super(name, null, Collections.emptyMap(), instancesSealed);
    this.constructor = constructor;
    this.methods = methods;
  }

  public InternalFunction getMethod(String name) {
    return methods.get(name);
  }

  public boolean methodExists(String name) {
    return methods.containsKey(name);
  }
  
  /**
   * Instantiates a RuntimeInstance with associated methods,
   * 
   * Note: This method DOESN'T invoke this RuntimeDataRecord's
   *       constructor, so the returned object isn't properly
   *       initialized.
   * @param allocator - the HeapAllocator to use for instantiation
   * @param hostModule - the RuntimeModule where instantiation is occuring
   * @return a RuntimeInstance with an intial mapping of associated methods.
   */
  @Override
  public RuntimeInstance instantiate(HeapAllocator allocator, RuntimeModule hostModule) {
    final RuntimeInstance selfObject = allocator.allocateEmptyObject((ini, self) -> {
      for (Entry<String, InternalFunction> method : methods.entrySet()) {
        ini.init(method.getKey(), allocator.allocateCallable(hostModule, self, method.getValue()));
      }

      ini.init(TYPE_ATTR, this);
    });

    return selfObject;
  }

  /**
   * Prepares this RuntimeDataRecord's constructor for invocation.
   * @param allocator - the HeapAllocator to use for preparation.
   * @param hostModule - the host module of this RuntimeDataRecord
   * @param instance - the instance of this RuntimeDateRecord
   * @return the constructor as a Callable.
   */
  @Override
  public Callable prepareConstructor(HeapAllocator allocator, RuntimeModule hostModule, RuntimeInstance instance) {
    return allocator.allocateCallable(hostModule, instance, constructor);
  }
}
