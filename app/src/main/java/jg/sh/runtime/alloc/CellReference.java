package jg.sh.runtime.alloc;

import jg.sh.runtime.objects.RuntimeInstance;

public class CellReference {

  private RuntimeInstance value;
  
  public CellReference(RuntimeInstance initValue) {
    this.value = initValue;
  }
  
  public void setValue(RuntimeInstance value) {
    this.value = value;
  }
  
  public RuntimeInstance getValue() {
    return value;
  }
}
