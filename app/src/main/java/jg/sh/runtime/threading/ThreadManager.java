package jg.sh.runtime.threading;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;

import jg.sh.InterpreterOptions.IOption;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;
import jg.sh.runtime.threading.pool.ThreadPool;
import jg.sh.runtime.threading.thread.RuntimeThread;

public class ThreadManager {
  
  private final Map<IOption, Object> options;
  
  private final HeapAllocator allocator;
  private final ModuleFinder finder;
  private final ThreadPool threadPool;
  private final ConcurrentHashMap<Integer, Fiber> allFibers;

  private final Lock completionLock;
  private final Condition complCond;

  private final Cleaner cleaner;
  
  private volatile boolean stopScheduling;
  
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
  
  public Fiber spinFiber(Callable callable, ArgVector vector) {
    Fiber executor = new Fiber(allocator, finder, this, cleaner);
    try {
      executor.queue(callable, vector);
      threadPool.queueFiber(executor);
      reportFiber(executor);     
    } catch (InvocationException e) {
      e.printStackTrace();
    }
    return executor;
  }
  
  public RuntimeThread makeThread(Callable callable) {
    RuntimeThread thread = new RuntimeThread(allocator, finder, cleaner, this, callable, this::reportFiber);    
    reportFiber(thread);
    return thread;
  }

  public void initialize() {
    threadPool.initialize();
  }
  
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
  
  public void stop() {
    stopScheduling = true;
    threadPool.stop();
  }

  public boolean hasStopped() {
    return stopScheduling;
  }
  
  public boolean poolHasJobs() {
    return threadPool.hasStopped();
  }

  public Map<IOption, Object> getOptions() {
    return options;
  }
}
