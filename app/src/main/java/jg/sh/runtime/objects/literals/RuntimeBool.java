package jg.sh.runtime.objects.literals;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;

public class RuntimeBool extends RuntimePrimitive {
  
  private static final InternalFunction EQUAL = 
  create(
    RuntimeBool.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value == otherBool.getValue());
      }
      return fiber.getHeapAllocator().allocateBool(false);
    }
  );

  private static final InternalFunction NOT_EQUAL = 
  create(
    RuntimeBool.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value != otherBool.getValue());
      }
      return fiber.getHeapAllocator().allocateBool(true);
    }
  );

  private static final InternalFunction NOT = 
  create(
    RuntimeBool.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateBool(!self.value);
    }
  );

  private static final InternalFunction BAND = 
  create(
    RuntimeBool.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value & otherBool.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction BOR = 
  create(
    RuntimeBool.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value | otherBool.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );
  
  private final boolean value;
  
  public RuntimeBool(boolean value) {
    super((self, m) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
      m.put(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, EQUAL));
      m.put(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT_EQUAL));
      m.put(FuncOperatorCoupling.NOT.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT));
      m.put(FuncOperatorCoupling.BAND.getFuncName(), new ImmediateInternalCallable(systemModule, self, BAND));
      m.put(FuncOperatorCoupling.BOR.getFuncName(), new ImmediateInternalCallable(systemModule, self, BOR));
    });
    this.value = value;
  }
  
  public boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
