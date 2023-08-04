package jg.sh.runtime.objects.literals;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

public class RuntimeInteger extends RuntimePrimitive {

  private final long value;
  
  public RuntimeInteger(long value) {
    super();
    this.value = value;
  }

  @Override
  public RuntimeInstance $add(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value + ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value + ((RuntimeFloat) otherOp).getValue());
    }
    else if(otherOp instanceof RuntimeString) {
      return alloc.allocateString(value + ((RuntimeString) otherOp).getValue());
    }
    throw new OperationException("+ not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $inc(HeapAllocator alloc) throws OperationException {
    return alloc.allocateInt(value + 1);
  }

  @Override
  public RuntimeInstance $dec(HeapAllocator alloc) throws OperationException {
    return alloc.allocateInt(value - 1);
  }
  

  public long getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
