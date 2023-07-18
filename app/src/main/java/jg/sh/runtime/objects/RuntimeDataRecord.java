package jg.sh.runtime.objects;

import java.util.Map;

import jg.sh.runtime.alloc.Cleaner;

/**
 * A data record is a template for constructing an 
 * object with a specified structure (methods, initial attributes, etc.)
 * 
 * Note: A RuntimeDateRecord is sealed after instantiation.
 */
public class RuntimeDataRecord extends RuntimeInstance {

  private final Map<String, RuntimeCodeObject> methods;
  private final String name;

  public RuntimeDataRecord(String name, Map<String, RuntimeCodeObject> methods, boolean isSealed) {
    this.name = name;
    this.methods = methods;
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

  public Map<String, RuntimeCodeObject> getMethods() {
    return methods;
  }
  
}
