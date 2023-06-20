package jg.sh.runtime.objects.callable;

import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;

public class RuntimeCallable extends Callable {
  
  private static final int STARTING_CLOSURE_SIZE = 1;

  private final RuntimeCodeObject codeObject;
  
  private CellReference [] captures;
  
  public RuntimeCallable(RuntimeModule hostModule, RuntimeInstance self, RuntimeCodeObject codeObject, CellReference [] captures) {
    super(codeObject.getSignature(), hostModule, self);
    this.codeObject = codeObject;
    this.captures = captures;
  }
  
  public RuntimeCallable(RuntimeModule hostModule, RuntimeInstance self, RuntimeCodeObject codeObject) {
    this(hostModule, self, codeObject, new CellReference[STARTING_CLOSURE_SIZE]);
  }
  
  public void setCapture(int captureIndex, RuntimeInstance value) {
    if (captureIndex >= captures.length) {
      //Increase the size of the localVars array
      final CellReference [] newLocals = new CellReference[captureIndex + 1];
      
      System.arraycopy(captures, 0, newLocals, 0, captures.length);
      
      captures = newLocals;
    }
    
    if (captures[captureIndex] == null) {
      captures[captureIndex] = new CellReference(value);
    }
    else {
      captures[captureIndex].setValue(value);
    }
  }
  
  public RuntimeInstance getCapture(int captureIndex) {
    return captures[captureIndex] == null ? null : captures[captureIndex].getValue();
  }

  public RuntimeCodeObject getCodeObject() {
    return codeObject;
  }
  
  public CellReference[] getCaptures() {
    return captures;
  }
  
  @Override
  public Callable rebind(RuntimeInstance newSelf, HeapAllocator allocator) {
    if (newSelf != getSelf()) {
      return allocator.allocateCallable(getHostModule(), newSelf, codeObject, captures);
    }
    return this;
  }
  
  @Override
  public void markAdditional(Cleaner cleaner) {
    cleaner.gcMarkObject(codeObject);
    
    for (CellReference cellReference : captures) {
      cleaner.gcMarkObject(cellReference.getValue());
    }
  }
}
