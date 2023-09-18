package jg.sh.runtime.threading.fiber;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.Initializer;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.runtime.threading.frames.FunctionFrame;
import jg.sh.runtime.threading.frames.JavaFrame;
import jg.sh.runtime.threading.frames.ReturnAction;
import jg.sh.runtime.threading.frames.StackFrame;
import jg.sh.runtime.threading.pool.ThreadPool;
import jg.sh.runtime.threading.stackops.ErrorContinuer;
import jg.sh.runtime.threading.stackops.TopModifier;
import jg.sh.runtime.threading.stackops.TopTwoModifier;
import jg.sh.util.RuntimeUtils;

import static jg.sh.runtime.objects.callable.InternalFunction.create;

/**
 * Fibers are a frame-steppable thread of execution. To further clarify, 
 * Fibers can be intermittently executed a frame a time without needing to executed
 * continously until call stack exhaustion - like a typical Thread implementation.
 * 
 * While threads are the main implementation of a program's execution in many languages
 * (such as in Java), Fibers are the default for Seahorse programs. Threds (isolated, concurrent
 * and continuous lines of execution) can still be used by Seahorse programs through
 * ({@link jg.sh.runtime.threading.thread.RuntimeThread})
 * 
 * A fiber is identified with a unique integer ID and two fibers
 * are deemed equal if they match IDs.
 * 
 * Note: While a Fiber is meant to be advanced by several threads, only one
 *       thread should be advance a Fiber at a time. 
 * 
 *       This class is inherently thread-unsafe and requires external locking
 *       and/or synchronization - hence why the {@link ThreadPool} manages the advancement
 *       of Fibers amongst its pool of threads
 *  
 * @author Jose
 */
public class Fiber extends RuntimeInstance {

  /**
   * Static counter for Fiber ID creation
   */
  private static volatile int FIBER_ID_COUNTER = 0;

  private static final InternalFunction START = 
  create(
    Fiber.class,
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      self.start();
      return RuntimeNull.NULL;
    }
  );

  private static final InternalFunction START_TIME_GETTER = 
  create(
    Fiber.class,
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      Fiber selfFiber = (Fiber) self;
      return fiber.getHeapAllocator().allocateInt(selfFiber.getStartTime());
    }
  );

  private static final InternalFunction END_TIME_GETTER = 
  create(
    Fiber.class,
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateInt(fiber.getEndTime());
    }
  );

  private static final InternalFunction FIBER_ID_GETTER = 
  create(
    Fiber.class,
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateInt(fiber.getFiberID());
    }
  );

  private static final InternalFunction FIBER_STATUS_GETTER = 
  create(
    Fiber.class,
    FunctionSignature.NO_ARG, 
    (fiber, self, callable, args) -> {
      return fiber.getHeapAllocator().allocateString(fiber.getStatus().name());
    }
  );
  
  private final HeapAllocator allocator;
  private final ModuleFinder finder;
  private final ThreadManager manager;
  private final Deque<StackFrame> callStack;
  private final Deque<RuntimeInstance> operandStack;
  private final Cleaner cleaner;
  private final int fiberID;
  protected final Consumer<Fiber> fiberReporter;
    
  private RuntimeInstance leftOver;
  private InvocationException leftOverException;

  /**
   * The time (in nanoseconds) this fiber had its first frame advanced - marking the start of this fiber
   * 
   * If -1, this fiber has yet to queued
   */
  private volatile long startTime;

  /**
   * The time (in nanoseconds) this fiber finished it last frame, effectively ending it.
   * 
   * If -1, this fiber has yet to complete.
   */
  private volatile long endTime;

  /**
   * Current status of this fiber
   */
  private volatile FiberStatus status;
    
  /**
   * Constructs a Fiber
   * @param allocator - the HeapAllocator to use for object allocation
   * @param finder - the ModuleFinder to use for loading modules
   * @param cleaner - the Cleaner to use for garbage collection
   * @param manager - the ThreadManager this RuntimeThread is managed by
   */
  public Fiber(HeapAllocator allocator, 
               ModuleFinder finder, 
               ThreadManager manager, 
               StackFrame initialFrame,
               Cleaner cleaner,
               Consumer<Fiber> fiberReporter,
               BiConsumer<Initializer, RuntimeInstance> initializer) {
    super((ini, self) -> {
      final RuntimeModule systemModule =  SystemModule.getNativeModule().getModule();

      ini.init("startTime", new ImmediateInternalCallable(systemModule, self, START_TIME_GETTER), AttrModifier.CONSTANT);
      ini.init("endTime", new ImmediateInternalCallable(systemModule, self, END_TIME_GETTER), AttrModifier.CONSTANT);
      ini.init("getID", new ImmediateInternalCallable(systemModule, self, FIBER_ID_GETTER), AttrModifier.CONSTANT);
      ini.init("getStatus", new ImmediateInternalCallable(systemModule, self, FIBER_STATUS_GETTER), AttrModifier.CONSTANT);
      ini.init("start", new ImmediateInternalCallable(systemModule, self, START), AttrModifier.CONSTANT);

      if (initializer != null) {
        initializer.accept(ini, self);
      }
    });
    
    this.allocator = allocator;
    this.finder = finder;
    this.manager = manager;
    this.cleaner = cleaner;
    this.callStack = new ArrayDeque<>();
    this.operandStack = new ArrayDeque<>();
    this.fiberID = FIBER_ID_COUNTER++;
    this.startTime = -1;
    this.endTime = -1;
    this.status = FiberStatus.CREATED;
    this.fiberReporter = fiberReporter;

    if (initialFrame != null) {
      queueFrame(initialFrame);
    }
  }

  /**
   * Queues this Fiber into the ThreadPool
   */
  public void start() {
    if (status == FiberStatus.CREATED) {
      manager.queueFiber(this);
    }
  }

  @Override
  public boolean equals(Object arg) {
    if (arg instanceof Fiber) {
      return ((Fiber) arg).fiberID == fiberID;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return fiberID;
  }

  public void setStatus(FiberStatus status) {
    this.status = status;
  }

  public void markEndTime() {
    endTime = System.nanoTime();
  }

  public StackFrame makeAndQueue(Callable callable, 
                                 ArgVector args, 
                                 HeapAllocator allocator,
                                 ReturnAction action) throws CallSiteException{
    final StackFrame frame = makeFrame(callable, args, allocator, action);
    queueFrame(frame);
    return frame;
  }

  public StackFrame makeFrame(Callable callable, 
                              ArgVector args, 
                              HeapAllocator allocator,
                              ReturnAction action) throws CallSiteException {    
    final FunctionSignature signature = callable.getSignature();

    /*
     * At Index 0 -> callable
     * At Index 1 -> self object
     */
    args.addAtFront(callable.getSelf());
    args.addAtFront(callable);
    
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
      toReturn = new JavaFrame(internalCallable.getHostModule(), internalCallable, args, action, this);
    }
    else {
      //System.out.println("CALLING!!!!! user space "+args.getPositionals().size());
      
      final RuntimeCallable regularCallable = (RuntimeCallable) callable;
      final RuntimeCodeObject codeObject = regularCallable.getCodeObject();
      final RuntimeInstance [] allPositionals = args.getPositionals().toArray(new RuntimeInstance[0]);

      if(!args.hasKeywords()) {
        /*
         * The ArgVector only has positionl arguments.
         *
         * Because of this, we don't have to process keyword arguments and can just immediately
         * return the StackFrame of the function with just the positional arguments
         */
        toReturn = new FunctionFrame(regularCallable.getHostModule(), regularCallable, 0, args, allPositionals, action, this);
      }
      else {
        /*
         * The ArgVector has keyword  
         */

        final int totalParams = 2 + signature.getPositionalParamCount() + 
                              signature.getKeywordParams().size() + 
                              (signature.hasVariableParams() ? 1 : 0) +
                              (signature.hasVarKeywordParams() ? 1 : 0);
        final RuntimeInstance [] paramLocals = new RuntimeInstance[totalParams];

        //All positional arguments:
        System.arraycopy(allPositionals, 0, paramLocals, 0, 2 + signature.getPositionalParamCount());
        

        //FunctionFrame frame = new FunctionFrame(regularCallable.getHostModule(), regularCallable, 0, args, action, this);
        //Push the new frame!
        //System.out.println("------> PUSHED FRAME "+args.getPositional(0));

        //Set positional arguments to the calle's local variables
        /*
        int positionalIndex = 0;
        for(; positionalIndex < signature.getPositionalParamCount() + 2 ; positionalIndex++) {
          //System.out.println(" -- setting local: "+positionalIndex+" with val: "+args.getPositional(positionalIndex));
          frame.storeLocalVar(positionalIndex, args.getPositional(positionalIndex));
        }
        */

        /**
         * We combine keyword arguments and extra keyword argument setting 
         * in one go, by readily allocating the keywordVarArg object and using it's 
         * initialization parameter to decide which keyword args go in keywordVarArg object
         * or be saved directly as a local variable
         */
        final Map<String, Integer> keywordToIndexMap = codeObject.getKeywordIndexes();
        final RuntimeInstance leftOverKeywords = allocator.allocateEmptyObject((ini, self) -> {
          for (Entry<String, RuntimeInstance> keywordArg : args.getKeywords().entrySet()) {
            if(keywordToIndexMap.containsKey(keywordArg.getKey())) {
              final int keywordIndex = keywordToIndexMap.get(keywordArg.getKey());
              //System.out.println("        ===> saving as local: "+keywordIndex);
              //frame.storeLocalVar(keywordIndex, keywordArg.getValue());
              paramLocals[keywordIndex] = keywordArg.getValue();
            }
            else if(!signature.getKeywordParams().contains(keywordArg.getKey())) {
              ini.init(keywordArg.getKey(), keywordArg.getValue());
            }
          }
        });

        if (signature.hasVarKeywordParams()) {
          //frame.storeLocalVar(codeObject.getKeywordVarArgIndex(), leftOverKeywords);
          paramLocals[regularCallable.getCodeObject().getKeywordVarArgIndex()] = leftOverKeywords;
        }
        
        
        //System.out.println("------------> DONE WITH ARGS");
        
        //Put any leftover positional arguments in an array
        if (signature.getPositionalParamCount() + 2 < allPositionals.length) {
          //final RuntimeArray leftOvers = allocator.allocateEmptyArray();

          final RuntimeInstance [] varArgsArray = Arrays.copyOfRange(allPositionals, signature.getPositionalParamCount() + 2, allPositionals.length);
          //final RuntimeInstance [] varArgsArray = new RuntimeInstance[allPositionals.length - signature.getPositionalParamCount() - 2];
          //System.arraycopy(allPositionals, signature.getPositionalParamCount() + 2, varArgsArray, 0, varArgsArray.length);
          System.out.println(" ==> var arg size: "+varArgsArray.length);

          //System.out.println(" ===> STARTING VARARGS: "+(positionalIndex + 1)+" | "+args.getPositionals().size()+" | "+args.getPositionals());
          /*
          for(int i = positionalIndex; i < args.getPositionals().size(); i++) {
            leftOvers.addValue(args.getPositional(i));
          }
          */

          //frame.storeLocalVar(regularCallable.getCodeObject().getVarArgIndex(), leftOvers);
          paramLocals[codeObject.getVarArgIndex()] = new RuntimeArray(varArgsArray);
        }
        
        toReturn = new FunctionFrame(regularCallable.getHostModule(), regularCallable, 0, args, paramLocals, action, this);
      }
    }
    
    return toReturn;
  }
 
  /**
   * Initializes this Executor to execute a RuntimeCallable.
   * @param callable - the RuntimeCallable to initialized this Executor with.
   * @args args - the arguments meant to pass to this RuntimeCallable
   */
  public void queueFrame(StackFrame frame) {
    pushFrame(frame);
  }

  /**
   * Advances this Fiber by one function frame.
   */
  public void advanceFrame() {
    if (!isCallStackEmpty()) {
      StackFrame topFrame = popFrame();

      //Set start time, if needed
      startTime = startTime < 0 ? System.nanoTime() : startTime;
      
      StackFrame current = topFrame.run(allocator);
      if (current == null) {
        //error flag set, or frame was done (returned value)
        if (topFrame.hasError()) {
          //error flag was set
          if (!isCallStackEmpty()) {
            peekFrame().returnError(topFrame.getError().getErrorObject());
          }
          else {
            leftOverException = topFrame.getError();
          }
        }
        else {
          //topFrame is done.
          if (!isCallStackEmpty()) {
            pushOperand(topFrame.popOperand());
          }
          else {
            //System.out.println("   -> got new frame: "+topFrame.hasOperand()+" | "+topFrame.hashCode()+" | "+topFrame.getClass());
            leftOver = topFrame.hasOperand() ? topFrame.popOperand() : null;
          }
        }    
      }
      else {
        if(!topFrame.isDone()) {
          pushFrame(topFrame);
        }
        pushFrame(current);
      }      
    }        
  }
  
  /*
   * PRIVATE UTILITY METHODS - start
   */

  private ArrayDeque<RuntimeInstance> newWholeDeque() {
    final ArrayDeque<RuntimeInstance> newDeque = new ArrayDeque<>(operandStack);

    if (t2 != null) {
      newDeque.push(t2);
    }
    if (t1 != null) {
      newDeque.push(t1);
    } 
    if(t0 != null) {
      newDeque.push(t0);
    }

    return newDeque;
  }
  
  /*
   * PRIVATE UTILITY METHODS - end 
   */

  /*
   * OPERAND STACK UTILITY METHODS - start 
   */
  private RuntimeInstance t0, t1, t2;

  public void pushOperand(RuntimeInstance value) {
    //System.out.println(" ==> pushing: "+value+" | stack: "+newWholeDeque());

    if (t2 == null) {
      t2 = value;
    }
    else if (t1 == null) {
      t1 = value;
    }
    else if (t0 == null) {
      t0 = value;
    }
    else {
      operandStack.push(t2);
      t2 = t1;
      t1 = t0;
      t0 = value;
    }
  }

  /**
   * Applies an operation on the top-two operands
   * and replaces the top operand with the result.
   * @param modifier - the TopModifer to invoke to modify the top-most operand
   * @param continuance - the StackFrame 
   * @param errorHandler
   * @return
   */
  public StackFrame modifyTopTwoOperand(TopTwoModifier modifier, 
                                        StackFrame continuance,
                                        ErrorContinuer errorHandler) {
    //System.out.println(" ===> whole stack: "+newWholeDeque());
    //System.out.println(" => t0: "+t0);
    //System.out.println(" => t1: "+t1);
    //System.out.println(" => t2: "+t2);

    if(t0 != null && t1 != null) {
      try {
        final RuntimeInstance temp_t1 = modifier.modify(t0, t1);

        if (temp_t1 == null) {
          return errorHandler.error(t0, t1, null);
        }

        t0 = null;
        t1 = temp_t1;
      } catch (OperationException e) {
        return errorHandler.error(t0, t1, e);
      }
    }
    else if(t1 != null && t2 != null) {
      try {
        final RuntimeInstance temp_t2 = modifier.modify(t1, t2);

        if (temp_t2 == null) {
          return errorHandler.error(t1, t2, null);
        }

        t1 = null;
        t2 = temp_t2;
      } catch (OperationException e) {
        return errorHandler.error(t1, t2, e);
      }
    }
    else if(t2 != null) {
      final RuntimeInstance secondTop = operandStack.pop();
      try {
        final RuntimeInstance temp_t2 = modifier.modify(t2, secondTop);

        if (temp_t2 == null) {
          return errorHandler.error(t2, secondTop, null);
        }

        t2 = temp_t2;
      } catch (OperationException e) {
        return errorHandler.error(t2, secondTop, e);
      }
    }
    else {
      final RuntimeInstance first = popOperand();
      final RuntimeInstance second = popOperand();

      try {
        final RuntimeInstance modified = modifier.modify(first, second);

        if (modified == null) {
          return errorHandler.error(first, second, null);
        }
        pushOperand(modified);
      } catch (OperationException e) {
        return errorHandler.error(first, second, e);
      }
    }

    return continuance;
  }

  /**
   * Modifies the top-operand "in-place".
   * 
   * "In-place" here means utilizing the cached top-three
   * elements of the operand stack to avoid actual
   * stack popping.
   * 
   * This method uses "t0" - the top element of the stack" - 
   * to modify it directly in place.
   * @param modifier - the TopModifer to invoke to modify the top-most operand
   * @param continuance - the StackFrame 
   * @param errorHandler
   * @return
   */
  public StackFrame modifyTopOperand(TopModifier modifier, 
                                     StackFrame continuance,
                                     BiFunction<RuntimeInstance, OperationException, StackFrame> errorHandler) {
    
    if(t0 != null) {
      try {
        final RuntimeInstance temp_t0 = modifier.modify(t0);

        if (temp_t0 == null) {
          return errorHandler.apply(t0, null);
        }
        t0 = temp_t0;
      } catch (OperationException e) {
        return errorHandler.apply(t0, e);
      }
    }
    else if(t1 != null) {
      try {
        final RuntimeInstance temp_t1 = modifier.modify(t1);

        if (temp_t1 == null) {
          return errorHandler.apply(t1, null);
        }

        t1 = temp_t1;
      } catch (OperationException e) {
        return errorHandler.apply(t1, e);
      }
    }
    else if(t2 != null) {
      try {
        final RuntimeInstance temp_t2 = modifier.modify(t2);

        if (temp_t2 == null) {
          return errorHandler.apply(t2, null);
        }

        t2 = temp_t2;
      } catch (OperationException e) {
        return errorHandler.apply(t2, e);
      }
    }
    else {
      final RuntimeInstance top = popOperand();
      try {
        final RuntimeInstance modified = modifier.modify(top);

        if (modified == null) {
          return errorHandler.apply(top, null);
        }
        pushOperand(modified);
      } catch (OperationException e) {
        return errorHandler.apply(top, e);
      }
    }

    return continuance;
  }

  public RuntimeInstance popOperand() {
    if(t0 != null) {
      final RuntimeInstance temp = t0;
      t0 = null;
      return temp;
    }
    else if(t1 != null) {
      final RuntimeInstance temp = t1;
      t1 = null;
      return temp;
    }
    else if(t2 != null) {
      final RuntimeInstance temp = t2;
      t2 = null;   
      return temp;
    }
    else {
      final RuntimeInstance res = operandStack.pop();

      //Pull up the top three from the inner stack
      t0 = operandStack.isEmpty() ? null : operandStack.pop();
      t1 = operandStack.isEmpty() ? null : operandStack.pop();
      t2 = operandStack.isEmpty() ? null : operandStack.pop();

      return res;
    }
  }
  
  public RuntimeInstance peekOperand() {
    return t0 == null ? (t1 == null ? (t2 == null ? operandStack.peek() : t2) : t1) : t0;
  }

  public Deque<RuntimeInstance> getFullOperandStack() {
    return newWholeDeque();
  }

  public void clearOpStack() {
    t0 = t1 = t2 = null;
    operandStack.clear();
  }

  public boolean isOpStackEmpty() {
    return t0 == null && t1 == null && t2 == null && operandStack.isEmpty();
  }

  /*
   * OPERAND STACK UTILITY METHODS - end 
   */

  /*
   * FRAME STACK UTILITY METHODS - start 
   */
  private StackFrame f0, f1, f2;

  private void pushFrame(StackFrame frame) {
    if(f0 == null) {
      f0 = frame;
    }
    else if(f1 == null) {
      f1 = f0;
      f0 = frame;
    }
    else if(f2 == null) {
      f2 = f1;
      f1 = f0;
      f0 = frame;
    }
    else {
      callStack.push(f2);
      f2 = f1;
      f1 = f0;
      f0 = frame;
    }
  }

  private StackFrame popFrame() {
    if(f0 != null) {
      final StackFrame temp = f0;
      f0 = null;
      return temp;
    }
    else if(f1 != null) {
      final StackFrame temp = f1;
      f1 = null;
      return temp;
    }
    else if(f2 != null) {
      final StackFrame temp = f2;
      f2 = null;   
      return temp;
    }
    else {
      final StackFrame res = callStack.pop();

      //Pull up the top three from the inner stack
      f0 = callStack.isEmpty() ? null : callStack.pop();
      f1 = callStack.isEmpty() ? null : callStack.pop();
      f2 = callStack.isEmpty() ? null : callStack.pop();

      return res;
    }
  }
  
  private StackFrame peekFrame() {
    return f0 == null ? (f1 == null ? (f2 == null ? callStack.peek() : f2) : f1) : f0;
  }

  private Deque<StackFrame> getFullCallStack() {
    final ArrayDeque<StackFrame> newDeque = new ArrayDeque<>(callStack);
    newDeque.push(f2);
    newDeque.push(f1);
    newDeque.push(f0);

    return newDeque;
  }

  private void clearCallStack() {
    f0 = f1 = f2 = null;
    callStack.clear();
  }

  private boolean isCallStackEmpty() {
    return f0 == null && f1 == null && f2 == null && callStack.isEmpty();
  }

  /*
   * FRAME STACK UTILITY METHODS - end 
   */

  public int getFiberID() {
    return fiberID;
  }

  /**
   * Returns this Executor's HeapAllocator
   * @return this Executor's HeapAllocator
   */
  public HeapAllocator getHeapAllocator() {
    return allocator;
  }

  /**
   * Checks if this Executor has a StackFrame in it's FunctionStack
   * @return true if a StackFrame is present in this Executor's FunctionStack, false if there isn't
   */
  public boolean hasFrame() {
    return !isCallStackEmpty();
  }

  /**
   * Returns the ThreadManager this Executor is managed by
   * @return the ThreadManager this Executor is managed by
   */
  public ThreadManager getManager() {
    return manager;
  }

  /**
   * Returns the RuntimeInstance as a result of executing the last
   * StackFrame in this Executor's FunctionStack
   * @return the RuntimeInstance as a result of executing the last
   * StackFrame in this Executor's FunctionStack
   */
  public RuntimeInstance getLastInstance() {
    return leftOver;
  }

  /**
   * Returns the unhandled InvocationException this fiber completed with.
   * @return the unhandled InvocationException this fiber completed with.
   */
  public InvocationException getLeftOverException() {
    return leftOverException;
  }

  /**
   * Whether this fiber has completed with a unhandled InvocationException
   * @return the unhandled InvocationException this fiber completed with.
   */
  public boolean hasLeftOverException() {
    return leftOverException != null;
  }
  
  /**
   * Returns the call stack of this Executor
   * @return the call stack of this Executor.
   */
  public Deque<StackFrame> getCallStack() {
    return getFullCallStack();
  }
  
  /**
   * Returns this Fiber's ModuleFinder
   * @return this Fiber's ModuleFinder
   */
  public ModuleFinder getFinder() {
    return finder;
  }

  /**
   * Returns the time (in miliseconds) that this Fiber's first
   * frame was advanced
   * @return the time (in miliseconds) that this Fiber's first
   * frame was advanced
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Returns the time (in miliseconds) that this Fiber completed its 
   * last frame, marking its termination
   * @return the time (in miliseconds) that this Fiber completed its 
   * last frame, marking its termination
   */
  public long getEndTime() {
    return endTime;
  }

  /**
   * Returns the current {@link FiberStatus} of this Fiber
   * @return the current {@link FiberStatus} of this Fiber
   */
  public FiberStatus getStatus() {
    return status;
  }

  /**
   * Returns this Fiber's operand stack
   * @return this Fiber's operand stack
   */
  public Deque<RuntimeInstance> getOperandStack() {
    return getFullOperandStack();
  }
}
