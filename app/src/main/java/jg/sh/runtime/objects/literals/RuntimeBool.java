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

public class RuntimeBool extends RuntimePrimitive {
  
  private static final InternalFunction EQUAL = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke(Fiber executor, ArgVector args) {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      RuntimeBool self = (RuntimeBool) args.getPositional(SELF_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value == otherBool.getValue());
      }
      return executor.getHeapAllocator().allocateBool(false);
    }   
  };
  
  private static final InternalFunction NOT_EQUAL = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke(Fiber executor, ArgVector args) {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      RuntimeBool self = (RuntimeBool) args.getPositional(SELF_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value != otherBool.getValue());
      }
      return executor.getHeapAllocator().allocateBool(false);
    }   
  };
  
  private static final InternalFunction NOT = new InternalFunction(FunctionSignature.NO_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) {
      RuntimeBool self = (RuntimeBool) args.getPositional(SELF_INDEX);
      return executor.getHeapAllocator().allocateBool(!self.value);
    }   
  };
  
  private static final InternalFunction BAND = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke( Fiber executor, ArgVector args) throws InvocationException {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      RuntimeBool self = (RuntimeBool) args.getPositional(SELF_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value & otherBool.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }   
  };
  
  private static final InternalFunction BOR = new InternalFunction(FunctionSignature.ONE_ARG) {
    @Override
    public RuntimeInstance invoke(Fiber executor, ArgVector args) throws InvocationException {
      RuntimeInstance otherOperand = args.getPositional(ARG_INDEX);
      RuntimeBool self = (RuntimeBool) args.getPositional(SELF_INDEX);
      if (otherOperand instanceof RuntimeBool) {
        RuntimeBool otherBool = (RuntimeBool) otherOperand;
        return executor.getHeapAllocator().allocateBool(self.value | otherBool.getValue());
      }
      
      throw new InvocationException("Unsupported operand on addition!", (Callable) args.getPositional(0));
    }   
  };  
  
  
  
  private final boolean value;
  
  public RuntimeBool(boolean value) {
    this.value = value;
    
    final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();
    
    attributes.put(FuncOperatorCoupling.EQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, this, EQUAL));
    attributes.put(FuncOperatorCoupling.NOTEQUAL.getFuncName(), new ImmediateInternalCallable(systemModule, this, NOT_EQUAL));
    attributes.put(FuncOperatorCoupling.NOT.getFuncName(), new ImmediateInternalCallable(systemModule, this, NOT));
    attributes.put(FuncOperatorCoupling.BAND.getFuncName(), new ImmediateInternalCallable(systemModule, this, BAND));
    attributes.put(FuncOperatorCoupling.BOR.getFuncName(), new ImmediateInternalCallable(systemModule, this, BOR));
  }
  
  public boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
