package jg.sh.runtime.objects;

import java.util.Map;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.objects.callable.RuntimeCallable;

/**
 * A data record is a template for constructing an 
 * object with a specified structure (methods, initial attributes, etc.)
 * 
 * Note: A RuntimeDateRecord is sealed after instantiation.
 */
public class RuntimeDataRecord extends RuntimeInstance {

  private final String name;

  public RuntimeDataRecord(String name, Map<String, RuntimeCodeObject> methods, boolean isSealed) {
    super((self, m) -> {
      m.putAll(methods);
    });
    this.name = name;
    seal();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof RuntimeDataRecord && ((RuntimeDataRecord) o).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getName() {
    return name;
  } 

  @Override
  protected void markAdditional(Cleaner allocator) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'markAdditional'");
  }
  
}
