package jg.sh.runtime.objects.literals;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

public class RuntimeString extends RuntimePrimitive {
  
  private static final InternalFunction ADD = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      //System.out.println("--- in add function for RuntimeString!!!");

      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        //System.out.println("---- adding?");
        return executor.getHeapAllocator().allocateString(self.value + otherStr.getValue());
      }
      else if (otherOperand instanceof RuntimeInteger) {
        RuntimeInteger otherInt = (RuntimeInteger) otherOperand;
        //System.out.println("---- adding? "+(self.value + otherInt.getValue()));
        return executor.getHeapAllocator().allocateString(self.value + otherInt.getValue());
      }
      else if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        //System.out.println("---- adding?");
        return executor.getHeapAllocator().allocateString(self.value + otherBool.getValue());
      } 
      else if (otherOperand instanceof RuntimeFloat) {
        RuntimeFloat otherFloat = (RuntimeFloat) otherOperand;
        //System.out.println("---- adding?");
        return executor.getHeapAllocator().allocateString(self.value + otherFloat.getValue());
      }
      
      //System.out.println("--- not correct type for other operand for add() in RuntimeString");
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  };
  
  private static final InternalFunction LESS = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) < 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  };
  
  private static final InternalFunction GREAT = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) > 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  };
  
  private static final InternalFunction LESSE = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) <= 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  };
  
  private static final InternalFunction GREATE = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.compareTo(otherStr.getValue()) >= 0);
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }
  };
  
  private static final InternalFunction EQUAL = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.equals(otherStr.getValue()));
      }
      
      return executor.getHeapAllocator().allocateBool(false);
    }
  };
  
  private static final InternalFunction NOT_EQUAL = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeString self = (RuntimeString) args.getPositional(SELF_INDEX);
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      if (otherOperand instanceof RuntimeString) {
        RuntimeString otherStr = (RuntimeString) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value.equals(otherStr.getValue()));
      }
      
      return executor.getHeapAllocator().allocateBool(true);
    }
  };
  

  private final String value;
  
  public RuntimeString(String value) {
    this.value = value;

    final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();

    attributes.put(FuncOperatorCoupling.ADD.getFuncName(), new RuntimeInternalCallable(systemModule, this, ADD));

    attributes.put(FuncOperatorCoupling.LESS.getFuncName(), new RuntimeInternalCallable(systemModule, this, LESS));

    attributes.put(FuncOperatorCoupling.GREAT.getFuncName(), new RuntimeInternalCallable(systemModule, this, GREAT));

    attributes.put(FuncOperatorCoupling.LESSE.getFuncName(), new RuntimeInternalCallable(systemModule, this, LESSE));

    attributes.put(FuncOperatorCoupling.GREATE.getFuncName(), new RuntimeInternalCallable(systemModule, this, GREATE));

    attributes.put(FuncOperatorCoupling.EQUAL.getFuncName(), new RuntimeInternalCallable(systemModule, this, EQUAL));

    attributes.put(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new RuntimeInternalCallable(systemModule, this, NOT_EQUAL));
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return value;
  }
}
