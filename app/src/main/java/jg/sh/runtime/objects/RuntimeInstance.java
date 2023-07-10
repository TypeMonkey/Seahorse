package jg.sh.runtime.objects;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.exceptions.SealedObjectException;

/**
 * Root type representing all runtime entities.
 * 
 * A RuntimeInstance is backed by a Map of attributes.
 */
public abstract class RuntimeInstance implements Markable {
    
  protected final Map<String, RuntimeInstance> attributes;
  
  private volatile boolean isSealed;

  private int gcFlag;

  public RuntimeInstance(BiConsumer<RuntimeInstance, Map<String, RuntimeInstance>> initializer) {
    this();
    initializer.accept(this, attributes);
  }
  
  public RuntimeInstance() {
    this.attributes = new ConcurrentHashMap<>();
    this.gcFlag = Cleaner.GC_UNMARK_VALUE;
  }
    
  public void setAttribute(String name, RuntimeInstance valueAddr) throws SealedObjectException {
    if (isSealed) {
      throw new SealedObjectException(name);
    }
    else {
      attributes.put(name, valueAddr);
    }
  }
  
  public RuntimeInstance getAttr(String name) {
    return attributes.get(name);
  }

  public void seal() {
    this.isSealed = true;
  }
  
  public boolean hasAttr(String name) {
    return attributes.containsKey(name);
  }
  
  public void setGcFlag(int gcFlag) {
    this.gcFlag = gcFlag;
  }

  public boolean isSealed() {
    return isSealed;
  }
  
  public int getGcFlag() {
    return gcFlag;
  }
  
  @Override
  public void gcMark(Cleaner allocator) {
    for (RuntimeInstance attr : attributes.values()) {
      allocator.gcMarkObject(attr);
    }
    
    markAdditional(allocator);
  }
  
  protected abstract void markAdditional(Cleaner allocator);
  
  public Map<String, RuntimeInstance> getAttributes() {
    return attributes;
  }
  
  /*
  @Override
  public void finalize(){
     System.out.println(" Being colltect: "+this);
  }
  */
}
