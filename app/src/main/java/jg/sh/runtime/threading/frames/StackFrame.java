package jg.sh.runtime.threading.frames;

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

  protected final Stack<RuntimeInstance> operandStack;
  protected final ArgVector initialArgs;
  
  protected RuntimeInstance [] localVars; 
  protected InvocationException error; // null <- no error, anything else <- error object
  
  private volatile boolean isDone;
  private int gcFlag;
  
  public StackFrame(RuntimeModule hostModule, 
                    ArgVector initialArgs) {
    this.localVars = new RuntimeInstance[0];
    this.operandStack = new Stack<>();
    this.initialArgs = initialArgs;
  } 
    
  public abstract StackFrame run(HeapAllocator allocator, Fiber thread);
    
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
  }
  
  public void pushOperand(RuntimeInstance value) {
    operandStack.push(value);
  }

  public boolean isDone() {
    return isDone;
  }
  
  public RuntimeInstance popOperand() {
    return operandStack.pop();
  }
  
  public RuntimeInstance peekOperand() {
    return operandStack.peek();
  }
  
  public boolean hasOperand() {
    return !operandStack.isEmpty();
  }
  
  public Stack<RuntimeInstance> getOperandStack() {
    return operandStack;
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
  }
  
  protected abstract void markAdditional(Cleaner allocator);
  
  public InvocationException getError() {
    return error;
  }
  
  public boolean hasError() {
    return this.error != null;
  }
  
  public void clearOpStack() {
    operandStack.clear();
  }
  
  public abstract Callable getCallable();
  
  public static StackFrame makeFrame(Callable callable, 
                                     ArgVector args, 
                                     HeapAllocator allocator) throws CallSiteException {

    /*
     * At Index 0 -> callable
     * At Index 1 -> self object
     */
    args.addAtFront(callable.getSelf());
    args.addAtFront(callable);
    
    final FunctionSignature signature = callable.getSignature();
    
    /*
     * Check if arguments are valid first!
     */
    final CallSiteException exception = RuntimeUtils.checkArgs(callable, signature, args);
    if (exception != null) {
      throw exception;
    }
    
    StackFrame toReturn = null;
    
    //Now, do the calling!
    if (callable instanceof RuntimeInternalCallable) {
      //System.out.println("CALLING!!!!! internal ");

      RuntimeInternalCallable internalCallable = (RuntimeInternalCallable) callable;
      toReturn = new JavaFrame(internalCallable.getHostModule(), internalCallable, args);
    }
    else {
      //System.out.println("CALLING!!!!! user space "+args.getPositionals().size());
      
      RuntimeCallable regularCallable = (RuntimeCallable) callable;

      FunctionFrame frame = new FunctionFrame(regularCallable.getHostModule(), regularCallable, 0, args);
      //Push the new frame!
      //System.out.println("------> PUSHED FRAME "+args.getPositional(0));

      //Set positional arguments to the calle's local variables
      int positionalIndex = 0;
      for(; positionalIndex < signature.getPositionalParamCount() + 2 ; positionalIndex++) {
        //System.out.println(" -- setting local: "+positionalIndex+" with val: "+args.getPositional(positionalIndex));
        frame.storeLocalVar(positionalIndex, args.getPositional(positionalIndex));
      }
      
      /**
       * We combine keyword arguments and extra keyword argument setting 
       * in one go, by readily allocating the keywordVarArg object and using it's 
       * initialization parameter to decide which keyword args go in keywordVarArg object
       * or be saved directly as a local variable
       */
      final Map<String, Integer> keywordToIndexMap = regularCallable.getCodeObject().getKeywordIndexes();
      final RuntimeInstance leftOverKeywords = allocator.allocateEmptyObject((ini, self) -> {
        for (Entry<String, RuntimeInstance> keywordArg : args.getAttributes().entrySet()) {
          if(keywordToIndexMap.containsKey(keywordArg.getKey())) {
            int keywordIndex = keywordToIndexMap.get(keywordArg.getKey());
            //System.out.println("        ===> saving as local: "+keywordIndex);
            frame.storeLocalVar(keywordIndex, keywordArg.getValue());
          }
          else if(!signature.getKeywordParams().contains(keywordArg.getKey())) {
            ini.init(keywordArg.getKey(), keywordArg.getValue());
          }
        }
      });

      if (signature.hasVarKeywordParams()) {
        frame.storeLocalVar(regularCallable.getCodeObject().getKeywordVarArgIndex(), leftOverKeywords);
      }
      
      //System.out.println("------------> DONE WITH ARGS");
      
      //Put any leftover positional arguments in an array
      if (signature.hasVariableParams()) {
        final RuntimeArray leftOvers = allocator.allocateEmptyArray();
        //System.out.println(" ===> STARTING VARARGS: "+(positionalIndex + 1)+" | "+args.getPositionals().size()+" | "+args.getPositionals());
        for(int i = positionalIndex; i < args.getPositionals().size(); i++) {
          leftOvers.addValue(args.getPositional(i));
        }

        frame.storeLocalVar(regularCallable.getCodeObject().getVarArgIndex(), leftOvers);
      }
      
      toReturn = frame;
    }
    
    return toReturn;
  }
}
