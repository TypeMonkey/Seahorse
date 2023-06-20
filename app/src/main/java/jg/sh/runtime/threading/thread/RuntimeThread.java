package jg.sh.runtime.threading.thread;

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
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.fiber.FiberStatus;

public class RuntimeThread extends Fiber {

  private static final InternalFunction START = new InternalFunction(FunctionSignature.NO_ARG) {
      @Override
      public RuntimeInstance invoke(Fiber fiber, ArgVector args) throws InvocationException {
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
    };
  
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
    
    final RuntimeModule systemModule = SystemModule.getNativeModule().getModule();
    
    setAttribute("start", new RuntimeInternalCallable(systemModule, this, START));
  }
  
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
  
  public void start() {
    thread.start();
  }
  
}
