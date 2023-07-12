package jg.sh.runtime.objects.literals;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.callable.InternalFunction;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;

public class RuntimeInteger extends RuntimePrimitive {
  
  private static final InternalFunction ADD = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateInt(self.value + otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value + otherFloat.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction SUB = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateInt(self.value - otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value - otherFloat.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction MUL = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateInt(self.value * otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value * otherFloat.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction DIV = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      try {
        if (otherOperand instanceof RuntimeInteger) {
          RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
          return fiber.getHeapAllocator().allocateInt(self.value / otherInt.getValue());
        }
        else if (otherOperand instanceof RuntimeFloat) {
          RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
          return fiber.getHeapAllocator().allocateFloat(self.value / otherFloat.getValue());
        }
        
        throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
      } catch (ArithmeticException e) {
        throw new InvocationException(e.getMessage(), callable);
      }
    }
  );

  private static final InternalFunction MOD = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      try {
        if (otherOperand instanceof RuntimeInteger) {
          RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
          return fiber.getHeapAllocator().allocateInt(self.value % otherInt.getValue());
        }
        else if (otherOperand instanceof RuntimeFloat) {
          RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
          return fiber.getHeapAllocator().allocateFloat(self.value % otherFloat.getValue());
        }
        
        throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
      } catch (ArithmeticException e) {
        throw new InvocationException(e.getMessage(), callable);
      }
    }
  );

  private static final InternalFunction NEG = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateInt( -self.getValue() );      
    }
  );

  private static final InternalFunction LESS = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value < otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value < otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction GREAT = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value > otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value > otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction LESSE = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value <= otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value <= otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction GREATE = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value >= otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value >= otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction EQUAL = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value == otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value == ((long) otherFloat.getValue()));
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction NOT_EQUAL = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value != otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value != ((long) otherFloat.getValue()));
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction BAND = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateInt(self.value & otherInt.value);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );

  private static final InternalFunction BOR = 
  create(
    RuntimeInteger.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateInt(self.value | otherInt.value);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  );
  
  private final long value;
  
  public RuntimeInteger(long value) {
    super((self, m) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
      m.put(FuncOperatorCoupling.ADD.getFuncName(), new ImmediateInternalCallable(systemModule, self, ADD));
      m.put(FuncOperatorCoupling.SUB.getFuncName(), new ImmediateInternalCallable(systemModule, self, SUB));
      m.put(FuncOperatorCoupling.MUL.getFuncName(), new ImmediateInternalCallable(systemModule, self, MUL));
      m.put(FuncOperatorCoupling.DIV.getFuncName(), new ImmediateInternalCallable(systemModule, self, DIV));
      m.put(FuncOperatorCoupling.MOD.getFuncName(), new ImmediateInternalCallable(systemModule, self, MOD));
      m.put(FuncOperatorCoupling.NEG.getFuncName(), new ImmediateInternalCallable(systemModule, self, NEG));
      m.put(FuncOperatorCoupling.LESS.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESS));
      m.put(FuncOperatorCoupling.GREAT.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREAT));
      m.put(FuncOperatorCoupling.LESSE.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESSE));
      m.put(FuncOperatorCoupling.GREATE.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREATE));
      m.put(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, EQUAL));
      m.put(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT_EQUAL));
      m.put(FuncOperatorCoupling.BAND.getFuncName(), new ImmediateInternalCallable(systemModule, self, BAND));
      m.put(FuncOperatorCoupling.BOR.getFuncName(), new ImmediateInternalCallable(systemModule, self, BOR));
    });
    this.value = value;
  }

  public long getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
