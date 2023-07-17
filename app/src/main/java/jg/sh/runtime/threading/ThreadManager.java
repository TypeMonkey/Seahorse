package jg.sh.runtime.threading;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import jg.sh.InterpreterOptions.IOption;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;
import jg.sh.runtime.threading.frames.StackFrame;
import jg.sh.runtime.threading.pool.ThreadPool;
import jg.sh.runtime.threading.thread.RuntimeThread;

/**
 * Manages the threads and fibers of the Seahorse interpreter.
 */
public class ThreadManager {
  
  private final Map<IOption, Object> options;
  
  private final HeapAllocator allocator;
  private final ModuleFinder finder;
  private final ThreadPool threadPool;
  private final Cleaner cleaner;
  private final ConcurrentHashMap<Integer, Fiber> allFibers;

  private final Lock completionLock;
  private final Condition complCond;
  
  private volatile boolean stopScheduling;
  
  /**
   * Constructs a ThreadManager.
   * @param allocator - the HeapAllocator to use
   * @param finder - the ModuleFinder to use
   * @param cleaner - the Cleaner to invoke for garbage collection
   * @param options - the interpreter options to abide by
   */
  public ThreadManager(HeapAllocator allocator, ModuleFinder finder, Cleaner cleaner, Map<IOption, Object> options) {
    this.allocator = allocator;
    this.finder = finder;
    this.options = options;
    this.allFibers = new ConcurrentHashMap<>();
    this.cleaner = cleaner;
    this.threadPool = new ThreadPool((int) options.get(IOption.POOL_SIZE), this::reportFiber);

    this.completionLock = new ReentrantLock();
    this.complCond = completionLock.newCondition();
  }

  /**
   * A method passed to the worker threads (as a BiConsumer lambda) 
   * to signal that a Fiber has completed - or terminated - its execution, meaning
   * that it has exhausted all its function frames
   * @param f - the completed/terminated Fiber
   * @param s - the FiberStatus (FiberStatus.COMPLETED or FiberStatus.TERMINATED) the given 
   *            Fiber has ended with.
   * 
   * Note: if the Fiber has a FiberStatus other than COMPLETED or TERMINATED, and it
   *       hasn't been added to the fiber map, then this method will add that fiber to the fiber map.
   */
  private void reportFiber(Fiber f){
    allFibers.putIfAbsent(f.getFiberID(), f);

    if (f.getStatus() == FiberStatus.COMPLETED || 
        f.getStatus() == FiberStatus.TERMINATED) {
      allFibers.remove(f.getFiberID());

      if (allFibers.isEmpty()) {
        //Notify that all fibers have been completed

        completionLock.lock();
        try {
          complCond.signal();
          stop();
        } finally {
          completionLock.unlock();
        }
      }
    }
  }

  public void queueFiber(Fiber fiber) {
    fiber.setStatus(FiberStatus.IN_QUEUE);
    threadPool.queueFiber(fiber);
  }
  
  /**
   * Creates and schedules a Fiber for execution
   * @param callable - the function to execute on this Fiber
   * @param vector - the ArgVector for the function
   * @return the created Fiber
   */
  public Fiber spinFiber(Callable callable, ArgVector vector) throws CallSiteException {
    final StackFrame initialFrame = StackFrame.makeFrame(callable, vector, allocator);
    Fiber executor = new Fiber(allocator, finder, this, initialFrame, cleaner, this::reportFiber, null);
    threadPool.queueFiber(executor);
    reportFiber(executor);     
    return executor;
  }

  /**
   * Creates a Fiber to be managed by this ThreadManager
   * @param callable - the function to run on this thread
   * @return the created {@link Fiber}
   * 
   * Note: Unlike {@link spinFiber}, the created {@link Fiber} isn't started immediately.
   */
  public Fiber makeFiber(Callable callable, ArgVector vector) throws CallSiteException {
    final StackFrame initialFrame = StackFrame.makeFrame(callable, vector, allocator);
    Fiber fiber = new Fiber(allocator, finder, this, initialFrame, cleaner, this::reportFiber, null);
    reportFiber(fiber);     
    return fiber;
  }
  
  /**
   * Creates a RuntimeThread to be managed by this ThreadManager
   * @param callable - the function to run on this thread
   * @return the created {@link RuntimeThread}
   * 
   * Note: Unlike {@link spinFiber}, the created {@link RuntimeThread} isn't started immediately.
   */
  public RuntimeThread makeThread(Callable callable, ArgVector vector) throws CallSiteException{
    final StackFrame initialFrame = StackFrame.makeFrame(callable, vector, allocator);
    RuntimeThread thread = new RuntimeThread(allocator, finder, cleaner, this, initialFrame, this::reportFiber);    
    reportFiber(thread);
    return thread;
  }

  /**
   * Initialize this ThreadManager.
   * 
   * Note: this method must be called prior to {@link start}.
   */
  public void initialize() {
    threadPool.initialize();
  }
  
  /**
   * Starts this ThreadManager.
   * 
   * @param blockUntilDone - if true, this method will block until all {@link Fiber} - including {@link RuntimeThread}
   *                         have been terminated or completed.
   */
  public void start(boolean blockUntilDone) {    
    threadPool.start();

    if (blockUntilDone) {
      /*
       * Wait caller until all fibers have
       * been reported to be complete/terminated
       */

      completionLock.lock();
      try {
        while (!allFibers.isEmpty()) {
          complCond.await();
        }
      } catch (Exception e) {
         completionLock.unlock();
      }
    }
  }
  
  /**
   * Stops all scheduled Fibers.
   * 
   * Note: this doesn't stop any running {@link RuntimeThread}.
   */
  public void stop() {
    stopScheduling = true;
    threadPool.stop();
  }

  /**
   * Whether this ThreadManager has been stopped
   * @return true if {@link stop()} has been called, false if else.
   */
  public boolean hasStopped() {
    return stopScheduling;
  }

  /**
   * Returns the map of interpreter options provided to this ThreadManager
   * @return the map of interpreter options provided to this ThreadManager
   */
  public Map<IOption, Object> getOptions() {
    return options;
  }
}
