package jg.sh.runtime.threading.thread;

import java.util.function.Consumer;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.SELF_INDEX;


/**
 * A thread of execution.
 * 
 * Unlike a {@link Fiber}, a RuntimeThread is backed by a {@link java.lang.Thread} 
 * which - when {@link start} is called - will execute it without waiting on 
 * the Seahorse threadpool. 
 */
public class RuntimeThread extends Fiber {

  private static final InternalFunction START = create(FunctionSignature.NO_ARG, 
    (fiber, args) -> {
      final Fiber selfFiber = (Fiber) args.getPositional(SELF_INDEX);
      if (selfFiber instanceof RuntimeThread) {
        final RuntimeThread thread = (RuntimeThread) selfFiber;
        thread.start();
        return RuntimeNull.NULL;
      }
      else {
        throw new InvocationException("Unsupported operand on start()", (Callable) args.getPositional(0));
      }
    }
  );
  
  private final Callable callable;
  private final ArgVector args;
  private final Thread thread;
  private final Consumer<Fiber> fiberReporter;

  public RuntimeThread(HeapAllocator allocator, 
                       ModuleFinder finder, 
                       Cleaner cleaner, 
                       ThreadManager manager,
                       Consumer<Fiber> fiberReporter) {
    this(allocator, finder, cleaner, manager, null, null, fiberReporter);
  }
  
  public RuntimeThread(HeapAllocator allocator, 
                       ModuleFinder finder, 
                       Cleaner cleaner, 
                       ThreadManager manager, 
                       Callable callable,
                       Consumer<Fiber> fiberReporter) {
    this(allocator, finder, cleaner, manager, callable, new ArgVector(), fiberReporter);
  }

  /**
   * Constructs a RuntimeThread
   * @param allocator - the HeapAllocator to use for object allocation
   * @param finder - the ModuleFinder to use for loading modules
   * @param cleaner - the Cleaner to use for garbage collection
   * @param manager - the ThreadManager this RuntimeThread is managed by
   * @param callable - the function to run on this thread
   * @param args - the arguments for the provided function
   * @param fiberReporter - the function to use to report that status of this RuntimeThread
   */
  public RuntimeThread(HeapAllocator allocator, 
                       ModuleFinder finder, 
                       Cleaner cleaner, 
                       ThreadManager manager, 
                       Callable callable, 
                       ArgVector args,
                       Consumer<Fiber> fiberReporter) {
    super(allocator, finder, manager, cleaner);
    this.callable = callable;
    this.args = args;
    this.thread = new Thread(this::startInternal);
    this.fiberReporter = fiberReporter;    

    setAttribute("start", new ImmediateInternalCallable(SystemModule.getNativeModule().getModule(), this, START));
  }
  
  /**
   * Executes the frames of this RuntimeThread
   * to completion.
   */
  private void startInternal() {
    try {
      queue(callable, args);

      setStatus(FiberStatus.RUNNING);
      while (hasFrame()) {
        advanceFrame();
      }

      markEndTime();
      setStatus(hasLeftOverException() ? FiberStatus.TERMINATED : FiberStatus.COMPLETED);
      fiberReporter.accept(this);

      if (hasLeftOverException()) {
        System.out.println("--- CAUGHT ERROR ");
        getLeftOverException().printStackTrace();
      }
    } catch (InvocationException e) {
      setStatus(FiberStatus.TERMINATED);
      fiberReporter.accept(this);
      System.err.println("Unhandled error on thread: "+e.getMessage());
    }
  }
  
  /**
   * Starts this RuntimeThread
   */
  public void start() {
    thread.start();
  }
  
}
