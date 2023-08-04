package jg.sh.runtime.objects.literals;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

public final class RuntimeString extends RuntimePrimitive {  

  private final String value;
  
  public RuntimeString(String value) {
    super();
    this.value = value;
  }

  @Override
  public RuntimeInstance $add(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    return alloc.allocateString(value + otherOp.toString());
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return value;
  }
}
