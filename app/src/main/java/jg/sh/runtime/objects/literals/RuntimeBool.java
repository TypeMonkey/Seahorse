package jg.sh.runtime.objects.literals;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

public final class RuntimeBool extends RuntimePrimitive {
  
  private final boolean value;
  
  public RuntimeBool(boolean value) {
    super();
    this.value = value;
  }

  public boolean equals(Object obj) {
    return obj instanceof RuntimeBool && ((RuntimeBool) obj).value == value;
  }

  @Override
  public RuntimeInstance $not(HeapAllocator alloc) throws OperationException {
    return alloc.allocateBool(!value);
  }

  @Override
  public RuntimeInstance $band(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeBool) {
      return alloc.allocateBool(value & ((RuntimeBool) otherOp).value);
    }
    throw new OperationException("& not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $bor(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateBool(value | ((RuntimeBool) otherOp).value);
    }
    throw new OperationException("| not applicable on type "+otherOp.getClass());
  }
  
  public boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
