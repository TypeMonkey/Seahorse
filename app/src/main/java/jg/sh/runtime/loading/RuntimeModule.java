package jg.sh.runtime.loading;

import java.util.Map;

import org.checkerframework.checker.units.qual.s;

import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.OpCode;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.metrics.GeneralMetrics;
import jg.sh.runtime.metrics.GeneralMetrics.Meaures;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;

public class RuntimeModule implements Markable {
  
  /*
  public static enum LoadingStatus {
    READ,
    LOADING,
    LOADED;
  }
  */
  
  private final String name;
  private final RuntimeCodeObject moduleCodeObject;
  private final RuntimeInstance [] constants;
  
  private Callable moduleCallable;
  private RuntimeInstance moduleObject;
  private boolean isLoaded;
  
  private int gcMark;
  
  public RuntimeModule(String name, 
                       RuntimeCodeObject codeObject,
                       RuntimeInstance [] constants) {
    this.name = name;
    this.moduleCodeObject = codeObject;
    this.constants = constants;
  }
  
  public void setLoadingComponents(RuntimeInstance moduleObject, Callable callable) {
    this.moduleCallable = callable;
    this.moduleObject = moduleObject;
  }
  
  public void setAsLoaded(boolean isLoaded) {
    this.isLoaded = isLoaded;
  }
  
  public boolean isLoaded() {
    return moduleObject != null && moduleCallable != null && isLoaded;
  }

  public RuntimeInstance getConstant(int index) {
    final long start = System.nanoTime();
    final RuntimeInstance constant = constants[index];
    GeneralMetrics.addTimes(Meaures.CONST_RETR, System.nanoTime() - start);
    return constant;
  }
  
  public RuntimeInstance [] getConstants() {
    return constants;
  }
  
  public RuntimeInstance getModuleObject() {
    return moduleObject;
  }
  
  public Callable getModuleCallable() {
    return moduleCallable;
  }
  
  public RuntimeCodeObject getModuleCodeObject() {
    return moduleCodeObject;
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public void setGcFlag(int gcFlag) {
    this.gcMark = gcFlag;
  }
  
  @Override
  public int getGcFlag() {
    return gcMark;
  }

  @Override
  public void gcMark(Cleaner allocator) {}

  @Override
  public String toString() {
    String x = "Runtime module '"+name+"'"+System.lineSeparator();
    
    x += "-------CONSTANT POOL-------"+System.lineSeparator();
    for(int i = 0; i < constants.length; i++) {
      x += "  "+i+" : "+constants[i]+System.lineSeparator();
    }
    x += "-------CONSTANT POOL-------"+System.lineSeparator();
    
    if (moduleCodeObject != null) {
      x += "CODE: "+System.lineSeparator();
      for (RuntimeInstruction instruction : moduleCodeObject.getInstrs()) {
        if (instruction.getOpCode() == OpCode.LABEL) {
          x += "   "+instruction+System.lineSeparator();
        }
        else {
          x += "        "+instruction+System.lineSeparator();
        }
      }
    }
    else {
      x += "CODE: None, module is native.";
    }
    
    return x;
  }
}
