package jg.sh.runtime.alloc;

import java.util.Stack;

import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.runtime.threading.frames.StackFrame;

public interface Cleaner {
  
  public static final int GC_MARK_VALUE = 1;
  public static final int GC_UNMARK_VALUE = 0;


  public void gc(Stack<StackFrame> callStack, HeapAllocator allocator, Fiber executor);
  
  public void gcMarkObject(Markable instance);
}
