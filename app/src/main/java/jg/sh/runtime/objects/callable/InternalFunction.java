package jg.sh.runtime.objects.callable;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.threading.fiber.Fiber;

public abstract class InternalFunction {
  
  /**
   * Index for the Callable instance that represents 
   * the function being invoked
   */
  public static final int FUNC_INDEX = 0;
  
  /**
   * Index for the self object that the function being invoked
   * is bound to.
   */
  public static final int SELF_INDEX = 1;
  
  /**
   * The start of positional function arguments. 
   * 
   * The 0th index is the function itself.
   * The 1st index is the "self" object
   */
  public static final int ARG_INDEX = 2;
  
  private final FunctionSignature signature;
  
  public InternalFunction(FunctionSignature signature) {
    this.signature = signature;
  }

  public abstract RuntimeInstance invoke(Fiber thread, ArgVector args) throws InvocationException;
  
  public FunctionSignature getSignature() {
    return signature;
  }

  /**
   * Convenient utility method for making InternalFunction using the InternalFuncInterface
   * @param signature - signature of this function
   * @param func - the actual code of this function
   * @return an InternalFunction 
   */
  public static InternalFunction create(FunctionSignature signature, InternalFuncInterface func) {
    return new InternalFunction(signature) {
      public RuntimeInstance invoke(Fiber thread, ArgVector args) throws InvocationException {
        return func.call(thread, args);
      }
    };
  }
}
