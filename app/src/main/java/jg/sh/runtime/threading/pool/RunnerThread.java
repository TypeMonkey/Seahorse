package jg.sh.runtime.threading.pool;

import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;

/**
 * A worker thread that executes a Fiber from a task queue
 * one frame at a time.
 */
public class RunnerThread extends Thread {

  private static final int FRAME_AMOUNT = 10;
  private static int THREAD_ID_COUNTER = 1;

  private static Logger LOG = LogManager.getLogger(RunnerThread.class);

  private final Supplier<Fiber> fiberSupplier;
  private final Consumer<Fiber> fiberConsumer;
  private final int id;

  private volatile boolean stop;

  /**
   * Whether this RunnerThread should keep executing
   * its current Fiber
   */
  private volatile boolean keepFiber;
    
  /**
   * Constructs a RunnerThread
   * @param taskQueue - the Queue to pull Fibers to work on
   * @param fiberCompleter - the Consumer to report Fibers that have completed/terminated
   */
  public RunnerThread(Supplier<Fiber> fiberSupplier, Consumer<Fiber> fiberConsumer) {
    this.fiberSupplier = fiberSupplier;
    this.fiberConsumer = fiberConsumer;
    this.id = THREAD_ID_COUNTER++;
    this.keepFiber = false;
    setName("Runner Thread "+this.id);
  }

  /**
   * Sets the stop-flag of this RunnerThread
   * @param stop - if true, this RunnerThread will stop checking for workable Fibers
   *               from the given Fiber queue. Otherwise, this RunnerThread will resume checking.
   */
  public void stop(boolean stop){
    this.stop = stop;
  }

  public void keepFiber(boolean value) {
    this.keepFiber = value;
  }

  @Override
  public void run() {
    while (!stop) {
      final Fiber exec = fiberSupplier.get();
      //System.out.println(" === pass over swithc! "+id);

      if (exec != null) {
        //Set fiber status to running
        exec.setStatus(FiberStatus.RUNNING);

        int frameCount = 0;
        /*
         * Keep advancing this Fiber until the FRAME_AMOUNT, unless
         * the RunnerThread has been allowed to keep running this Fiber
         * (if keepFiber is true) 
         */
        while ((keepFiber || frameCount < FRAME_AMOUNT) && exec.hasFrame()) {
          exec.advanceFrame();
          frameCount++;
        }

        if (exec.hasFrame()) {
          //If the fiber has a pending frame, add it back to the taskqueue
          exec.setStatus(FiberStatus.IN_QUEUE);
          fiberConsumer.accept(exec);
        }
        else {
          //The fiber has no more pending frames, marking its completion
          exec.markEndTime();

          //If the fiber completed with a left over excpetion, report it.
          exec.setStatus(exec.hasLeftOverException() ? FiberStatus.TERMINATED : FiberStatus.COMPLETED);

          if (exec.hasLeftOverException()) {
            System.err.println("Uncaught error: ");
            exec.getLeftOverException().printStackTrace();
          }

          fiberConsumer.accept(exec);
        }


        /* 
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

          if (exec.hasLeftOverException()) {
            System.err.println("Uncaught error: ");
            exec.getLeftOverException().printStackTrace();
          }

          fiberCompleter.accept(exec);
        }
        */
      }
    }
    //System.out.println(" ----> Runner thread stopped!!");
  }

  public int getRunnerId() {
    return id;
  }

  public boolean isKeepingFiber() {
    return keepFiber;
  }
}
