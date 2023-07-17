package jg.sh.runtime.threading.pool;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import jg.sh.runtime.threading.fiber.Fiber;

/**
 * A pool of worker threads (instances of RunnerThread) that
 * advance queued Fibers by a certain amount of frames.
 */
public class ThreadPool {
    
  private final RunnerThread [] runners;

  private final ConcurrentLinkedQueue<Fiber> fiberQueue;

  private final Consumer<Fiber> fiberReporter;

  private boolean hasBeenInitialized;
  private boolean hasStarted;
  private boolean hasStopped;

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
        runners[i] = new RunnerThread(fiberQueue, fiberReporter);
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
