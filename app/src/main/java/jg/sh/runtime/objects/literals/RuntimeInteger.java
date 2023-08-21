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

  public boolean equals(Object obj) {
    return obj instanceof RuntimeInteger && ((RuntimeInteger) obj).value == value;
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
  public RuntimeInstance $sub(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value - ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value - ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("- not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $mul(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value * ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value * ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("* not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $div(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value / ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value / ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("/ not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $mod(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value % ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateFloat(value % ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("% not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $neg(HeapAllocator alloc) throws OperationException {
    return alloc.allocateInt(-value);
  }

  @Override
  public RuntimeBool $less(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateBool(value < ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateBool(value < ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("< not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeBool $great(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateBool(value > ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateBool(value > ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("> not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeBool $lesse(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateBool(value <= ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateBool(value <= ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException("<= not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeBool $greate(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateBool(value >= ((RuntimeInteger) otherOp).value);
    }
    else if(otherOp instanceof RuntimeFloat) {
      return alloc.allocateBool(value >= ((RuntimeFloat) otherOp).getValue());
    }
    throw new OperationException(">= not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $band(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value & ((RuntimeInteger) otherOp).value);
    }
    throw new OperationException("& not applicable on type "+otherOp.getClass());
  }

  @Override
  public RuntimeInstance $bor(RuntimeInstance otherOp, HeapAllocator alloc) throws OperationException {
    if(otherOp instanceof RuntimeInteger) {
      return alloc.allocateInt(value | ((RuntimeInteger) otherOp).value);
    }
    throw new OperationException("| not applicable on type "+otherOp.getClass());
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
