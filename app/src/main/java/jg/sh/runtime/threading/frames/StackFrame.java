package jg.sh.runtime.threading.frames;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.Map.Entry;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;

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
  protected final Callable callable;
  protected final CompletableFuture<RuntimeInstance> returnValue;
  protected final ArgVector initialArgs;
  
  protected RuntimeInstance [] localVars; 
  protected InvocationException error; // null <- no error, anything else <- error object
  
  private int gcFlag;
  
  public StackFrame(RuntimeModule hostModule, 
                    Callable callable, 
                    ArgVector initialArgs, 
                    BiConsumer<RuntimeInstance, Throwable> atCompletion) {
    this.callable = callable;
    this.returnValue = new CompletableFuture<>();
    this.localVars = new RuntimeInstance[0];
    this.operandStack = new Stack<>();
    this.initialArgs = initialArgs;

    if(atCompletion != null){
      this.returnValue.whenComplete(atCompletion);
    }
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
    returnValue.complete(instance);
    pushOperand(instance);
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
    
  public void setErrorFlag(RuntimeError error) {
    final InvocationException invocationException = new InvocationException(error, callable);
    getFuture().completeExceptionally(invocationException);
    this.error = invocationException;
  }

  public void returnError(RuntimeError error) {
    setErrorFlag(error);
    getFuture().completeExceptionally(this.error);
  }
  
  public void pushOperand(RuntimeInstance value) {
    operandStack.push(value);
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
    return callable.getHostModule();
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
    cleaner.gcMarkObject(callable);
    
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
  
  /*
  public void registerNext(StackFrame nextFrame) {
    setNextFrame(nextFrame);
    nextFrame.setPrevFrame(this);
  }
  
  
  public void setNextFrame(StackFrame nextFrame) {
    this.nextFrame = nextFrame;
  }
  
  public void setPrevFrame(StackFrame prevFrame) {
    this.prevFrame = prevFrame;
  }
  
  public StackFrame getPrevFrame() {
    return prevFrame;
  }
  
  public StackFrame getNextFrame() {
    return nextFrame;
  }
  */
  
  public InvocationException getError() {
    return error;
  }
  
  public boolean hasError() {
    return getFuture().isCompletedExceptionally();
  }
  
  public void clearOpStack() {
    operandStack.clear();
  }
  
  public Callable getCallable() {
    return callable;
  }
  
  public CompletableFuture<RuntimeInstance> getFuture() {
    return returnValue;
  }

  public static StackFrame makeFrame(Callable callable, 
                                     ArgVector args, 
                                     HeapAllocator allocator) throws InvocationException {
    return makeFrame(callable, args, allocator, null);
  }
  
  public static StackFrame makeFrame(Callable callable, 
                                     ArgVector args, 
                                     HeapAllocator allocator, 
                                     BiConsumer<RuntimeInstance, Throwable> atCompletion) throws InvocationException {
    FunctionSignature signature = callable.getSignature();
    
    /*
     * Check if arguments are valid first!
     */
    
    //-2 from positional size as the first two arguments are self and the function itself
    if (args.getPositionals().size() - 2 < signature.getPositionalParamCount()) {
      throw new InvocationException("The function requires "+signature.getPositionalParamCount()+" positional arguments", callable);
    }
    if (args.getPositionals().size() - 2 > signature.getPositionalParamCount() && !signature.hasVariableParams()) {
      throw new InvocationException("Excess positional arguments. The function doesn't accept variable argument amount! "+args.getPositionals().size(), callable);
    }
    
    for (String argKey : args.getAttributes().keySet()) {
      if (!signature.getKeywordParams().contains(argKey)) {
        throw new InvocationException("Unknown keyword argument '"+argKey+"'", callable);
      }
    }
    
    StackFrame toReturn = null;
    
    //Now, do the calling!
    if (callable instanceof RuntimeInternalCallable) {
      //System.out.println("CALLING!!!!! internal ");

      RuntimeInternalCallable internalCallable = (RuntimeInternalCallable) callable;
      toReturn = new JavaFrame(internalCallable.getHostModule(), internalCallable, args, atCompletion);
    }
    else {
      //System.out.println("CALLING!!!!! user space "+args.getPositionals().size());
      
      RuntimeCallable regularCallable = (RuntimeCallable) callable;

      FunctionFrame frame = new FunctionFrame(regularCallable.getHostModule(), regularCallable, 0, args, atCompletion);
      //Push the new frame!
      //System.out.println("------> PUSHED FRAME "+args.getPositional(0));

      //Set positional arguments to the calle's local variables
      int positionalIndex = 0;
      for(; positionalIndex < signature.getPositionalParamCount() + 2 ; positionalIndex++) {
        //System.out.println(" -- setting local: "+positionalIndex+" with val: "+args.getPositional(positionalIndex));
        frame.storeLocalVar(positionalIndex, args.getPositional(positionalIndex));
      }
      
      Map<String, Integer> keywordToIndexMap = regularCallable.getCodeObject().getKeywordIndexes();
      for(Entry<String, RuntimeInstance> keywordArg : args.getAttributes().entrySet()) {
        if(keywordToIndexMap.containsKey(keywordArg.getKey())) {
          int keywordIndex = keywordToIndexMap.get(keywordArg.getKey());
          frame.storeLocalVar(keywordIndex, keywordArg.getValue());
        }
        else {
          throw new InvocationException("Unknown keyword argument '"+keywordArg.getKey()+"'", callable);
        }
      }
      
      //System.out.println("------------> DONE WITH ARGS");
      
      //Put any leftover positional arguments in an array
      if (signature.hasVariableParams()) {
        final int variableArgsIndex = positionalIndex;

        if(positionalIndex < args.getPositionals().size()) {
                    
          RuntimeArray variableArgs = allocator.allocateEmptyArray();
          for( ; positionalIndex < args.getPositionals().size(); positionalIndex++) {
            variableArgs.addValue(args.getPositional(positionalIndex));
          }
          
          frame.storeLocalVar(variableArgsIndex, variableArgs);
        }
        else {
          frame.storeLocalVar(variableArgsIndex, null);
        }
      }
      
      
      toReturn = frame;
    }
    
    return toReturn;
  }
}
