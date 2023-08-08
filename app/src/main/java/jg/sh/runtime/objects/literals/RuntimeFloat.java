package jg.sh.runtime.objects.literals;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

public final class RuntimeFloat extends RuntimePrimitive {

  private final double value;
  
  public RuntimeFloat(double value) {
    super();
    this.value = value;
  }

  public boolean equals(Object obj) {
    return obj instanceof RuntimeFloat && ((RuntimeFloat) obj).value == value;
  }

  @Override
  public RuntimeInstance $add(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateFloat(value + ((RuntimeInteger) otherOp).getValue());
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value + ((RuntimeFloat) otherOp).value);
    }
    else if(otherOp instanceof RuntimeString) {
      return alloc.allocateString(value + ((RuntimeString) otherOp).getValue());
    }
    throw new OperationException("+ not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $inc(HeapAllocator alloc) throws OperationException {
    return alloc.allocateFloat(value + 1.0);
  }

  @Override
  public RuntimeInstance $dec(HeapAllocator alloc) throws OperationException {
    return alloc.allocateFloat(value - 1.0);
  }
  
  public double getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}