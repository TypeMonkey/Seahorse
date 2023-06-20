package jg.sh.runtime.threading.fiber;

public enum FiberStatus {
  /**
   * The Fiber has been created. It has yet to run.
   */
  CREATED,

  /**
   * The Fiber is currently running.
   */
  RUNNING,

  /**
   * The Fiber is currently waiting in the task queue of the interpreter's worker pool.
   */
  IN_QUEUE,

  /**
   * The Fiber has been termianted due to an uncaught exception.
   */
  TERMINATED,

  /**
   * The Fiber has exhausted all frames from it's call stack, completing
   * its execution.
   */
  COMPLETED;
}
