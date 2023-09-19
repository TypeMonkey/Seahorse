package jg.sh.runtime.objects;

import jg.sh.runtime.exceptions.OperationException;

/**
 * Represents the null value.
 * 
 * Note: there is only one RuntimeNull instance.
 */
public class RuntimeNull extends RuntimeInstance {
  
  public static final RuntimeNull NULL = new RuntimeNull();
  
  private RuntimeNull() {}

  public void setAttribute(String name, RuntimeInstance valueAddr) throws OperationException {
    throw new OperationException("Cannot set attributes of the null value");
  }
  
  public RuntimeInstance getAttr(String name) {
    throw new NullPointerException("The null value has no attributes");
  }
  
  public boolean hasAttr(String name) {
    throw new NullPointerException("The null value has no attributes");
  }
  
  @Override
  public String toString() {
    return "~null";
  }
}
