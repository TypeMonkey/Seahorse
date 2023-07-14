package jg.sh.runtime.threading.pool;

import java.util.Queue;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;

/**
 * A worker thread that executes a Fiber from a task queue
 * one frame at a time.
 */
public class RunnerThread extends Thread {

    private static Logger LOG = LogManager.getLogger(RunnerThread.class);

  private final Queue<Fiber> taskQueue;
  private final Consumer<Fiber> fiberCompleter;

  private volatile boolean stop;
    
  /**
   * Constructs a RunnerThread
   * @param taskQueue - the Queue to pull Fibers to work on
   * @param fiberCompleter - the Consumer to report Fibers that have completed/terminated
   */
  public RunnerThread(Queue<Fiber> taskQueue, Consumer<Fiber> fiberCompleter) {
    this.taskQueue = taskQueue;
    this.fiberCompleter = fiberCompleter;
  }

  /**
   * Sets the stop-flag of this RunnerThread
   * @param stop - if true, this RunnerThread will stop checking for workable Fibers
   *               from the given Fiber queue. Otherwise, this RunnerThread will resume checking.
   */
  public void stop(boolean stop){
    this.stop = stop;
  }

  @Override
  public void run() {
    while (!stop) {
      final Fiber exec = taskQueue.poll();

      if (exec != null) {
        //Set fiber status to running
        exec.setStatus(FiberStatus.RUNNING);

        //Advance fiber by one frame
        exec.advanceFrame();
        if(exec.hasFrame()) {
          //If the fiber has a pending frame, add it back to the taskqueue
          exec.setStatus(FiberStatus.IN_QUEUE);
          taskQueue.add(exec);
        }
        else {
          //The fiber has no more pending frames, marking its completion
          exec.markEndTime();

          //If the fiber completed with a left over excpetion, report it.
          exec.setStatus(exec.hasLeftOverException() ? FiberStatus.TERMINATED : FiberStatus.COMPLETED);
          fiberCompleter.accept(exec);

          if (exec.hasLeftOverException()) {
            LOG.info("--- CAUGHT ERROR ");
            exec.getLeftOverException().printStackTrace();
          }
        }
      }
    }
    //System.out.println(" ----> Runner thread stopped!!");
  }
}
