package jg.sh.runtime.objects;

import java.util.ArrayList;
import java.util.List;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;

public class RuntimeError extends RuntimeInstance {

  public static class FrameMark extends RuntimeInstance {

    public FrameMark(RuntimeInteger line, RuntimeInstance column, RuntimeString hostModule) {
      super((ini, self) -> {
        ini.init("name", column, AttrModifier.CONSTANT);
        ini.init("column", column, AttrModifier.CONSTANT);
        ini.init("hostModule", hostModule, AttrModifier.CONSTANT);
      });
    }
  }

  private final List<FrameMark> frameMarks;

  public RuntimeError() {
    this.frameMarks = new ArrayList<>();
  }

  public RuntimeError(RuntimeString message) {
    super((ini, self) -> {
      ini.init("msg", message);
    });
    this.frameMarks = new ArrayList<>();
  }
  
  public void addFrameMark(RuntimeInteger line, RuntimeInteger column, RuntimeString hostModule, HeapAllocator allocator) {
    frameMarks.add(new FrameMark(line, column, hostModule));
  }

  public String getMessage() {
    return getAttr("msg").toString();
  }

  public List<FrameMark> getFrameMarks() {
    return frameMarks;
  }
}
