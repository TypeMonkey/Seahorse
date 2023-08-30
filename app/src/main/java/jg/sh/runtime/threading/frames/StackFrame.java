package jg.sh.runtime.threading.frames;

import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.metrics.GeneralMetrics;
import jg.sh.runtime.metrics.GeneralMetrics.Meaures;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.util.RuntimeUtils;

/**
 * Represents a function's frame on the FunctionStack
 * 
 * A StackFrame can both represents a function invocation within SeaHorse
 * and the invocation of Java (Native) method.
 * 
 * Note: The invocation of a Java method is recorded as a SINGLE frame.
 *       If the Java method invokes subsequent Java methods, no frames
 *       are pushed on the function stack per method, unless that method
 *       invokes a SeaHorse function in which that function is recorded
 *       on the stack
 *       
 * A StackFrame is based around the Callable on which it's being invoked on and 
 * comes with:
 *   - Local variables, represented as a RuntimeInstance []
 *   - An operand stack, represented as a Stack<RuntimeInstance>
 *   - A flag for RuntimeError thrown within the frame - null means no exception
 *   - A reference to the frame before and after the current one
 * 
 * @author Jose
 *
 */
public abstract class StackFrame implements Markable {
  
  /**
   * Dictates the starting size of the localVars array
   */
  //private static final int LOCAL_VAR_INIT_AMOUNT = 1;

  protected final Fiber fiber;
  protected final ArgVector initialArgs;
  protected final ReturnAction action;
  
  protected RuntimeInstance [] localVars; 
  protected InvocationException error; // null <- no error, anything else <- error object
  
  private volatile boolean isDone;
  private int gcFlag;
  
  public StackFrame(RuntimeModule hostModule, 
                    ArgVector initialArgs,
                    ReturnAction action,
                    Fiber fiber) {
    this.localVars = new RuntimeInstance[0];
    this.fiber = fiber;
    this.initialArgs = initialArgs;
    this.action = action;
  } 
    
  public abstract StackFrame run(HeapAllocator allocator);
    
  /**
   * Meant to be called when a value is being returned by a stack frame.
   * 
   * If there's a previous frame, this method will push the instance on that
   * frame's operand stack. Else, it'll put in "leftOver" which can be retrieved
   * using getLeftOver()
   * 
   * @param instance
   */
  protected void returnValue(RuntimeInstance instance) {
    pushOperand(instance);

    if (action != null) {
      action.ret(instance, null);
    }

    this.isDone = true;
  }
  
  public RuntimeInstance getLocalVar(int varIndex) {
    return localVars[varIndex];
  }

  public void storeLocalVar(int varIndex, RuntimeInstance value) {
    if (varIndex >= localVars.length) {
      //Increase the size of the localVars array
      final RuntimeInstance [] newLocals = new RuntimeInstance[varIndex + 1];
      
      System.arraycopy(localVars, 0, newLocals, 0, localVars.length);
      
      localVars = newLocals;
    }
    
    //System.out.println("--- lvar store: "+localVars.length+" , "+varIndex+" , "+value+" , null? "+(value == null)+" | "+getClass());
    
    localVars[varIndex] = value;
  }

  public void returnError(RuntimeError error) {
    this.error = new InvocationException(error, getCallable());

    if (action != null) {
      action.ret(null, error);
    }

    this.isDone = true;
  }
  
  public void pushOperand(RuntimeInstance value) {
    fiber.pushOperand(value);
  }

  public RuntimeInstance popOperand() {
    return fiber.popOperand();
  }

  public RuntimeInstance peekOperand() {
    return fiber.peekOperand();
  }
  
  public boolean hasOperand() {
    return !fiber.isOpStackEmpty();
  }

  public boolean isDone() {
    return isDone;
  }
  
  public RuntimeInstance [] getLocalVars() {
    return localVars;
  }
  
  public RuntimeModule getHostModule() {
    return getCallable().getHostModule();
  }
  
  @Override
  public void setGcFlag(int gcFlag) {
    this.gcFlag = gcFlag;
  }
  
  @Override
  public int getGcFlag() {
    return gcFlag;
  }
  
  @Override
  public void gcMark(Cleaner cleaner) {
    /*
    cleaner.gcMarkObject(getCallable());
    
    //System.out.println(" marking frame: "+current.getClass()+"  "+current);

    
    //mark local variables
    for(RuntimeInstance value : localVars) {
      //System.out.println("  LVAR is null? "+(value == null));
      cleaner.gcMarkObject(value);
    }
          
    //mark all values in the operand stack
    for(RuntimeInstance value : operandStack) {
      //System.out.println("marking value: "+value);
      cleaner.gcMarkObject(value);
    }
    
    markAdditional(cleaner);
    */
  }
  
  protected abstract void markAdditional(Cleaner allocator);
  
  public InvocationException getError() {
    return error;
  }
  
  public boolean hasError() {
    return this.error != null;
  }
  
  public void clearOpStack() {
    fiber.clearOpStack();
  }

  public Fiber getFiber() {
    return fiber;
  }
  
  public abstract Callable getCallable();
}
