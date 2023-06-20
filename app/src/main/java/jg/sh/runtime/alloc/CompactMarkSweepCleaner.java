package jg.sh.runtime.alloc;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Stack;

import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.frames.StackFrame;

public class CompactMarkSweepCleaner implements Cleaner {
  
  @Override
  public void gc(Stack<StackFrame> callStack, HeapAllocator allocator, Fiber executor) {
    //System.out.println("   *** PROFILE POINT: Pre GC "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));

    //System.out.println("-- need to visit: "+callStack.size());
      
    if (!callStack.isEmpty()) {
      //System.gc();
      
      //mark(callStack, executor);
      //compact(allocator);
      
      //callStack.peek().setGcFlag(GC_UNMARK_VALUE);
    }
        
    //System.out.println("   *** PROFILE POINT: Post GC "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
  }
  
  private void compact(HeapAllocator allocator) {
    List<WeakReference<RuntimeInstance>> instances = allocator.getStorage();
    
    System.out.println("--- instances: "+instances.size());
    for(int i = instances.size() - 1; i >= 0; i--) {
      if (instances.get(i).get() == null) {
        instances.remove(i);
      }
      
      /*
      if (instances.get(i).getGcFlag() != GC_MARK_VALUE) {
        System.out.println("--- not marked!: "+instances.get(i));     
        instances.remove(i);
      }
      else {
        //System.out.println("--- marked!: "+instances.get(i));
        instances.get(i).setGcFlag(GC_UNMARK_VALUE);
      }
      \
      */
    }
    //System.out.println("--- instances DONE: "+instances.size());
  }

  private void mark(Stack<StackFrame> callStack, Fiber executor) {
    executor.getFinder().gcMark(this);
    
    //System.out.println("-- need to visit: "+callStack.size());
    for (StackFrame current : callStack) {
      gcMarkObject(current);
    }
  }

  @Override
  public void gcMarkObject(Markable instance) {
    if (instance.getGcFlag() != GC_MARK_VALUE) {
      instance.setGcFlag(GC_MARK_VALUE);
      instance.gcMark(this);   
    }
  }
}
