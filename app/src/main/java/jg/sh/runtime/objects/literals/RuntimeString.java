package jg.sh.runtime.objects.literals;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;

public class RuntimeString extends RuntimePrimitive {  
  
  private static final InternalFunction ADD = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);

      return fiber.getHeapAllocator().allocateString(self.value + otherOperand.toString());
    }
  );

  private static final InternalFunction LESS = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) < 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction GREAT = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) > 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction LESSE = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) <= 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction GREATE = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) >= 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction EQUAL = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value.equals(otherStr.getValue()));
      }
      
      return fiber.getHeapAllocator().allocateBool(false);
    }
  );

  private static final InternalFunction NOT_EQUAL = create(
    RuntimeString.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return fiber.getHeapAllocator().allocateBool(!self.value.equals(otherStr.getValue()));
      }
      
      return fiber.getHeapAllocator().allocateBool(true);
    }
  );
  

  private final String value;
  
  public RuntimeString(String value) {
    super((ini, self) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
      ini.init(FuncOperatorCoupling.ADD.getFuncName(), new ImmediateInternalCallable(systemModule, self, ADD));
      ini.init(FuncOperatorCoupling.LESS.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESS));
      ini.init(FuncOperatorCoupling.GREAT.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREAT));
      ini.init(FuncOperatorCoupling.LESSE.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESSE));
      ini.init(FuncOperatorCoupling.GREATE.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREATE));
      ini.init(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, EQUAL));
      ini.init(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT_EQUAL));
    });
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return value;
  }
}
