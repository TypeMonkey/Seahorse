package jg.sh.runtime.objects;

import java.util.Arrays;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.literals.RuntimeInteger;

import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.create;

public class RuntimeByteArray extends RuntimeInstance {

  private static final InternalFunction SIZE = 
  create(
    RuntimeByteArray.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateInt(self.size());
    }
  );

  private static final InternalFunction RETR_INDEX = 
  create(
    RuntimeArray.class,
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance index = args.getPositional(ARG_INDEX);
      if (index instanceof RuntimeInteger) {
        RuntimeInteger integer = (RuntimeInteger) index;
        return self.getValue((int) integer.getValue());
      }
      
      throw new InvocationException("Unsupported index type '"+index+"'", callable);
    }
  );

  private final byte [] array;
  
  public RuntimeByteArray(byte [] array) {
    super((ini, self) -> {
      RuntimeModule systemModule = SystemModule.getNativeModule().getModule();
    
      ini.init("size", new ImmediateInternalCallable(systemModule, self, SIZE));
      ini.init(RuntimeArray.RETR_INDEX_ATTR, new ImmediateInternalCallable(systemModule, self, RETR_INDEX));
    });

    this.array = array;
    seal();
  }

  public RuntimeInstance $add(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    if (otherOperand instanceof RuntimeByteArray) {
      final RuntimeByteArray otherByteArray = (RuntimeByteArray) otherOperand;
      final byte [] newArr = new byte[array.length + otherByteArray.array.length];

      System.arraycopy(array, 0, newArr, 0, array.length);
      System.arraycopy(otherByteArray.array, 0, newArr, array.length, otherByteArray.array.length);

      return alloc.allocateBytes(newArr);
    }
    throw new OperationException("Other operand isn't a byte array");
  }

  public synchronized byte getValue(int index) {
    return array[index];
  }

  public int size() {
    return array.length;
  }
  
  @Override
  public synchronized String toString() {
    return Arrays.toString(array);
  }
}
