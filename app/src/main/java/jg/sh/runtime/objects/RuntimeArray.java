package jg.sh.runtime.objects;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.objects.literals.RuntimeInteger;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.FUNC_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.SELF_INDEX;

/**
 * An extensible array of elements.
 * 
 * A RuntimeArray is thread-safe through synchronization, meaning
 * only one thread at a time can access add, change and retrieve values at time.
 */
public class RuntimeArray extends RuntimeInstance {
  
  public static final String RETR_INDEX_ATTR = "$getAt";
  public static final String STORE_INDEX_ATTR = "$setAt";

  private static final InternalFunction SIZE = create(FunctionSignature.NO_ARG, 
    (fiber, args) -> {
      RuntimeArray self = (RuntimeArray) args.getPositional(SELF_INDEX);
      return fiber.getHeapAllocator().allocateInt(self.size());
    }
  );
  
  private static final InternalFunction ADD = create(FunctionSignature.ONE_ARG, 
    (fiber, args) -> {
      RuntimeArray self = (RuntimeArray) args.getPositional(SELF_INDEX);
      self.addValue(args.getPositional(ARG_INDEX));
      
      return RuntimeNull.NULL;
    }
  );

  private static final InternalFunction RETR_INDEX = create(FunctionSignature.ONE_ARG, 
    (fiber, args) -> {
      RuntimeArray self = (RuntimeArray) args.getPositional(SELF_INDEX);
      RuntimeInstance index = args.getPositional(ARG_INDEX);
      if (index instanceof RuntimeInteger) {
        RuntimeInteger integer = (RuntimeInteger) index;
        return self.getValue((int) integer.getValue());
      }
      
      throw new InvocationException("Unsupported index type '"+index+"'", (Callable) args.getPositional(FUNC_INDEX));
    }
  );

  private static final InternalFunction STORE_INDEX = create(FunctionSignature.ONE_ARG, 
    (fiber, args) -> {
      RuntimeArray self = (RuntimeArray) args.getPositional(SELF_INDEX);
      RuntimeInstance index = args.getPositional(ARG_INDEX);
      if (index instanceof RuntimeInteger) {
        RuntimeInteger integer = (RuntimeInteger) index;
        self.setValue((int) integer.getValue(), args.getPositional(ARG_INDEX + 1));
        return RuntimeNull.NULL;
      }
      
      throw new InvocationException("Unsupported index type '"+index+"'", (Callable) args.getPositional(FUNC_INDEX));
    }
  );

  private static final InternalFunction TO_STRING = create(FunctionSignature.NO_ARG, 
    (fiber, args) -> {
      RuntimeArray self = (RuntimeArray) args.getPositional(SELF_INDEX);
      return fiber.getHeapAllocator().allocateString(self.toString());
    }
  );

  private final List<RuntimeInstance> array;
  
  public RuntimeArray() {
    array = new ArrayList<>();
    
    RuntimeModule systemModule = SystemModule.getNativeModule().getModule();
    
    setAttribute("size", new RuntimeInternalCallable(systemModule, this, SIZE));
    setAttribute("add", new RuntimeInternalCallable(systemModule, this, ADD));
    setAttribute("toString", new RuntimeInternalCallable(systemModule, this, TO_STRING));
    setAttribute(RETR_INDEX_ATTR, new RuntimeInternalCallable(systemModule, this, RETR_INDEX));
    setAttribute(STORE_INDEX_ATTR, new RuntimeInternalCallable(systemModule, this, STORE_INDEX));
  }
  
  public synchronized void addValue(RuntimeInstance valueLoc) {
    array.add(valueLoc);
  }
  
  public synchronized RuntimeInstance getValue(int index) {
    return array.get(index);
  }

  public synchronized void setValue(int index, RuntimeInstance valueLoc) {
    array.set(index, valueLoc);
  }
  
  public List<RuntimeInstance> getArray() {
    return array;
  }
  
  public synchronized int size() {
    return array.size();
  }

  @Override
  public void markAdditional(Cleaner cleaner) {
    for (RuntimeInstance arrValue : array) {
      cleaner.gcMarkObject(arrValue);
    }
  }
  
  @Override
  public synchronized String toString() {
    return array.toString();
  }
}
