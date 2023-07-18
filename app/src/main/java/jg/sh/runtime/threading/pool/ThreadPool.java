package jg.sh.runtime.threading.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import jg.sh.runtime.threading.fiber.Fiber;

/**
 * Manages a pool of worker threads (instances of RunnerThread) that
 * advance queued Fibers by a certain amount of frames.
 */
public class ThreadPool {
    
  private final RunnerThread [] runners;

  private final ConcurrentLinkedQueue<Fiber> fiberQueue;

  private final Consumer<Fiber> fiberReporter;

  private boolean hasBeenInitialized;
  private boolean hasStarted;
  private boolean hasStopped;

  private volatile int fiberCount;

  /**
   * Constructs a ThreadPool
   * @param poolSize - the amount of threads this ThreadPool is meant to manage
   */
  public ThreadPool(int poolSize, Consumer<Fiber> reporter) {
    this.runners = new RunnerThread[poolSize];
    this.fiberQueue = new ConcurrentLinkedQueue<>();
    this.fiberReporter = reporter;
  }

  /**
   * Queues the given Fiber in this pool's Fiber queue.
   * @param fiber - the Fiber to queue
   */
  public void queueFiber(Fiber fiber) {
    fiberQueue.offer(fiber);
    fiberCount++;
    manageKeepStatus();
    fiberReporter.accept(fiber);
  }

  /**
   * Removes a Fiber from the Fiber queue, or null
   * if the queue is empty.
   * @return a Fiber from the Fiber queue, or null
   * if the queue is empty.
   */
  public Fiber pollFiber() {
    final Fiber polled = fiberQueue.poll();
    if (polled != null) {
      fiberCount--;
    }
    manageKeepStatus();
    return polled;
  }

  private volatile int lastFiberCount;

  private void manageKeepStatus() {
    final int fiberCountSnapShot = fiberCount;
    if(lastFiberCount == fiberCountSnapShot){
      return;
    }
    lastFiberCount = fiberCountSnapShot;

    if (runners.length == fiberCountSnapShot) {
      //System.out.println("locking all!");
      setKeepFiberAllRunners(true);
    }
    else if(runners.length > fiberCountSnapShot) {
      //System.out.println("unlocking all! "+runners.length+" "+fiberCountSnapShot);
      setKeepFiberAllRunners(false);
    }
    else {
      //when runners.length < fiberCount
      setKeepFiberAllRunners(true);
      //System.out.println("unlocking "+runners[runners.length -1].getId());
      runners[runners.length - 1].keepFiber(false);
    }
  }

  private void setKeepFiberAllRunners(boolean value) {
    for (RunnerThread t : runners) {
      t.keepFiber(value);
    }
  }

  /**
   * Initializes this thread pool.
   * 
   * Note: if this thread pool has been initialized already, 
   *       this method has no effect
   */
  public void initialize() {
    if (!hasBeenInitialized) {
      for (int i = 0; i < runners.length; i++) {
        runners[i] = new RunnerThread(this::pollFiber, this::queueFiber);
      }

      this.hasBeenInitialized = true;
    }
  }

  /**
   * Starts the threads of this thread pool.
   */
  public void start() {
    if (!hasStarted && hasBeenInitialized) {
      for (RunnerThread runnerThread : runners) {
        runnerThread.start();
      }

      this.hasStarted = true;
    }
  }

  public void stop() {
    if (hasStarted && hasBeenInitialized) {
      fiberQueue.clear();

      for (RunnerThread runnerThread : runners) {
        runnerThread.stop(true);
      }
      this.hasStopped = true;
    }
  }

  public ConcurrentLinkedQueue<Fiber> getTaskQueue() {
      return fiberQueue;
  }

  public boolean hasStopped() {
    return hasStopped;
  }

  public boolean hasBeenInitialized() {
    return hasBeenInitialized;
  }
}
