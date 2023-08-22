package jg.sh.runtime.objects;

import java.util.Map;
import java.util.Map.Entry;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.RuntimeModule;

/**
 * A data record is a template for constructing an 
 * object with a specified structure (methods, initial attributes, etc.)
 * 
 * Note: A RuntimeDateRecord is sealed after instantiation.
 */
public class RuntimeDataRecord extends RuntimeInstance {

  private final Map<String, RuntimeCodeObject> methods;
  private final String name;
  private final boolean instancesSealed;

  public RuntimeDataRecord(String name, Map<String, RuntimeCodeObject> methods, boolean instancesSealed) {
    this.name = name;
    this.methods = methods;
    this.instancesSealed = instancesSealed;
    seal();
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
    });

    return selfObject;
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
  
}