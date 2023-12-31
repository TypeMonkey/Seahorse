package jg.sh.runtime.threading.fiber;

import java.util.Map;
import java.util.Stack;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.Initializer;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.runtime.threading.frames.StackFrame;
import jg.sh.runtime.threading.pool.ThreadPool;

import static jg.sh.runtime.objects.callable.InternalFunction.create;

/**
 * Fibers frame-steppable threads of execution.
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
  private final Stack<StackFrame> callStack;
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
    this.callStack = new Stack<>();
    this.fiberID = FIBER_ID_COUNTER++;
    this.startTime = -1;
    this.endTime = -1;
    this.status = FiberStatus.CREATED;
    this.fiberReporter = fiberReporter;

    queue(initialFrame);
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
 
  /**
   * Initializes this Executor to execute a RuntimeCallable.
   * @param callable - the RuntimeCallable to initialized this Executor with.
   * @args args - the arguments meant to pass to this RuntimeCallable
   */
  public void queue(StackFrame frame) {
    callStack.push(frame);
  }

  /**
   * Advances this Fiber by one function frame.
   */
  public void advanceFrame() {
    if (!callStack.isEmpty()) {
      StackFrame topFrame = callStack.pop();

      //Set start time, if needed
      startTime = startTime < 0 ? System.nanoTime() : startTime;
      
      StackFrame current = topFrame.run(allocator, this);
      if (current == null) {
        //error flag set, or frame was done (returned value)
        if (topFrame.hasError()) {
          //error flag was set
          if (!callStack.isEmpty()) {
            callStack.peek().returnError(topFrame.getError().getErrorObject());
          }
          else {
            leftOverException = topFrame.getError();
          }
        }
        else {
          //topFrame is done.
          if (!callStack.isEmpty()) {
            callStack.peek().pushOperand(topFrame.popOperand());
          }
          else {
            //System.out.println("   -> got new frame: "+topFrame.hasOperand()+" | "+topFrame.hashCode()+" | "+topFrame.getClass());
            leftOver = topFrame.hasOperand() ? topFrame.popOperand() : null;
          }
        }    
      }
      else {
        if(!topFrame.isDone()) {
          callStack.push(topFrame);
        }
        callStack.push(current);
      }      
    }        
  }
  
  /*
   * PRIVATE UTILITY METHODS - start
   */

  
  /*
   * PRIVATE UTILITY METHODS - end 
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
    return !callStack.isEmpty();
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
  public Stack<StackFrame> getCallStack() {
    return callStack;
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
}
