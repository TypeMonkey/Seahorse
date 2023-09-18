package jg.sh.runtime.objects;

import java.util.Map;
import java.util.Map.Entry;

import jg.sh.parsing.token.TokenType;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.callable.Callable;

/**
 * A data record is a template for constructing an 
 * object with a specified structure (methods, initial attributes, etc.)
 * 
 * Note: A RuntimeDateRecord is sealed after instantiation.
 */
public class RuntimeDataRecord extends RuntimeInstance {

  public static final String TYPE_ATTR = "$type";

  private final Map<String, RuntimeCodeObject> methods;
  private final RuntimeCodeObject constructor;
  private final String name;
  private final boolean instancesSealed;

  public RuntimeDataRecord(String name, RuntimeCodeObject constructor, Map<String, RuntimeCodeObject> methods, boolean instancesSealed) {
    this.name = name;
    this.methods = methods;
    this.instancesSealed = instancesSealed;
    this.constructor = constructor;
    seal();
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
  public RuntimeInstance instantiate(HeapAllocator allocator, RuntimeModule hostModule) {
    final RuntimeInstance selfObject = allocator.allocateEmptyObject((ini, self) -> {
      for (Entry<String, RuntimeCodeObject> method : methods.entrySet()) {
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
  public Callable prepareConstructor(HeapAllocator allocator, RuntimeModule hostModule, RuntimeInstance instance) {
    return allocator.allocateCallable(hostModule, instance, constructor);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof RuntimeDataRecord && ((RuntimeDataRecord) o).name.equals(name);
  }

  public boolean areInstancesSealed() {
    return instancesSealed;
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getName() {
    return name;
  } 

  public Map<String, RuntimeCodeObject> getMethods() {
    return methods;
  }
  
  public RuntimeCodeObject getConstructor() {
    return constructor;
  }
}