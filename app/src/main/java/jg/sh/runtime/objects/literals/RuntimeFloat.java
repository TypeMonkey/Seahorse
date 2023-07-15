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

public class RuntimeFloat extends RuntimePrimitive {

  private static final InternalFunction ADD = 
  create(
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value + otherInt.getValue());
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
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value - otherInt.getValue());
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
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateFloat(self.value * otherInt.getValue());
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
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      try {
        if (otherOperand instanceof RuntimeInteger) {
          RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
          return fiber.getHeapAllocator().allocateFloat(self.value / otherInt.getValue());
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
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      try {
        if (otherOperand instanceof RuntimeInteger) {
          RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
          return fiber.getHeapAllocator().allocateFloat(self.value % otherInt.getValue());
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
    RuntimeFloat.class, 
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateFloat( -self.getValue() );      
    }
  );

  private static final InternalFunction LESS = 
  create(
    RuntimeFloat.class, 
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
    RuntimeFloat.class, 
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
    RuntimeFloat.class, 
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
    RuntimeFloat.class, 
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
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(((long) self.value) == otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value == otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private static final InternalFunction NOT_EQUAL = 
  create(
    RuntimeFloat.class, 
    FunctionSignature.ONE_ARG, 
    (fiber, self, callable, args) -> {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        return fiber.getHeapAllocator().allocateBool(((long) self.value) != otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        return fiber.getHeapAllocator().allocateBool(self.value != otherFloat.getValue());
      }

      throw new InvocationException("Unsupported operand on addition!", callable);
    }
  );

  private final double value;
  
  public RuntimeFloat(double value) {
    super((ini, self) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
      ini.init(FuncOperatorCoupling.ADD.getFuncName(), new ImmediateInternalCallable(systemModule, self, ADD));
      ini.init(FuncOperatorCoupling.SUB.getFuncName(), new ImmediateInternalCallable(systemModule, self, SUB));
      ini.init(FuncOperatorCoupling.MUL.getFuncName(), new ImmediateInternalCallable(systemModule, self, MUL));
      ini.init(FuncOperatorCoupling.DIV.getFuncName(), new ImmediateInternalCallable(systemModule, self, DIV));
      ini.init(FuncOperatorCoupling.MOD.getFuncName(), new ImmediateInternalCallable(systemModule, self, MOD));
      ini.init(FuncOperatorCoupling.NEG.getFuncName(), new ImmediateInternalCallable(systemModule, self, NEG));
      ini.init(FuncOperatorCoupling.LESS.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESS));
      ini.init(FuncOperatorCoupling.GREAT.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREAT));
      ini.init(FuncOperatorCoupling.LESSE.getFuncName(), new ImmediateInternalCallable(systemModule, self, LESSE));
      ini.init(FuncOperatorCoupling.GREATE.getFuncName(), new ImmediateInternalCallable(systemModule, self, GREATE));
      ini.init(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, EQUAL));
      ini.init(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, self, NOT_EQUAL));
    });
    this.value = value;
  }
  
  public double getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}