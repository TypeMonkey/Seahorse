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
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        //System.out.println("---- adding?");
        return fiber.getHeapAllocator().allocateString(self.value + otherStr.getValue());
      }
      else if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        //System.out.println("---- adding? "+(self.value + otherInt.getValue()));
        return fiber.getHeapAllocator().allocateString(self.value + otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        //System.out.println("---- adding?");
        return fiber.getHeapAllocator().allocateString(self.value + otherBool.getValue());
      } 
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        //System.out.println("---- adding?");
        return fiber.getHeapAllocator().allocateString(self.value + otherFloat.getValue());
      }
      
      //System.out.println("--- not correct type for other operand for add() in RuntimeString");
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
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
    super((self, m) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
      m.put(FuncOperatorCoupling.ADD.getFuncName(), new ImmediateInternalCallable(systemModule, self, ADD));
      m.put(FuncOperatorCoupling.LESS.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESS));
      m.put(FuncOperatorCoupling.GREAT.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREAT));
      m.put(FuncOperatorCoupling.LESSE.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESSE));
      m.put(FuncOperatorCoupling.GREATE.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREATE));
      m.put(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, EQUAL));
      m.put(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT_EQUAL));
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
