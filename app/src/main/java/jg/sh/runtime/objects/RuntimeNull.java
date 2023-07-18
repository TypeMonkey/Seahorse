package jg.sh.runtime.objects;

import jg.sh.runtime.alloc.Cleaner;

/**
 * Represents the null value.
 * 
 * Note: there is only one RuntimeNull instance.
 */
public class RuntimeNull extends RuntimeInstance {
  
  public static final RuntimeNull NULL = new RuntimeNull();
  
  private RuntimeNull() {}

  public void setAttribute(String name, RuntimeInstance valueAddr) {
    throw new UnsupportedOperationException("Cannot set attributes of the null value");
  }
  
  public RuntimeInstance getAttr(String name) {
    throw new NullPointerException("The null value has no attributes");
  }
  
  public boolean hasAttr(String name) {
    return false;
  }
  
  @Override
  public String toString() {
    return "null";
  }
}
