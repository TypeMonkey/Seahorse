package jg.sh.runtime.objects;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.objects.literals.RuntimeString;

public class RuntimeError extends RuntimeArray {

  public RuntimeError() {
    this(null);
  }

  public RuntimeError(RuntimeString message) {
    setAttribute("msg", message);
  }
  
  public void addFrameMark(int line, int column, String hostModule, HeapAllocator allocator) {
    RuntimeObject frameMark = allocator.allocateEmptyObject();
    frameMark.setAttribute("line", allocator.allocateInt(line));
    frameMark.setAttribute("column", allocator.allocateInt(line));
    frameMark.setAttribute("hostModule", allocator.allocateString(hostModule));
    addValue(frameMark);
  }

}
