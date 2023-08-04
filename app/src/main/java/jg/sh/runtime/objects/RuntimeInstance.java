package jg.sh.runtime.objects;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.metrics.GeneralMetrics;
import jg.sh.runtime.metrics.GeneralMetrics.Meaures;

/**
 * Root type representing all runtime entities.
 * 
 * A RuntimeInstance is backed by a Map of attributes.
 */
public class RuntimeInstance implements Markable {

  public static enum AttrModifier {
    CONSTANT,
    EXPORT;
  }
    
  private final Map<String, RuntimeInstance> attributes;
  private final Map<String, EnumSet<AttrModifier>> attrModifiers;
  
  private volatile boolean isSealed;

  private int gcFlag;

  public RuntimeInstance(BiConsumer<Initializer, RuntimeInstance> initializer) {
    this();

    if (initializer != null) {
      initializer.accept((n, value, mods) -> {
        attributes.put(n, value);
        attrModifiers.put(n, mods.length == 0 ? EnumSet.noneOf(AttrModifier.class) : EnumSet.copyOf(Arrays.asList(mods)));
      }, this);
    }
  }
  
  public RuntimeInstance() {
    this.attributes = new ConcurrentHashMap<>();
    this.attrModifiers = new ConcurrentHashMap<>();
    this.gcFlag = Cleaner.GC_UNMARK_VALUE;
  }
    
  public void setAttribute(String name, RuntimeInstance valueAddr, AttrModifier ... modifiers) throws OperationException {
    setAttribute(name, valueAddr, Arrays.asList(modifiers));
  }

  public void setAttribute(String name, RuntimeInstance valueAddr, Collection<AttrModifier> modifiers) throws OperationException {
    final EnumSet<AttrModifier> EMPTY_ATTRMODS = EnumSet.noneOf(AttrModifier.class);
    if (isSealed) {
      throw new OperationException("The object is sealed and immutable");
    }
    else if(attrModifiers.getOrDefault(name, EMPTY_ATTRMODS).contains(AttrModifier.CONSTANT)) {
      throw new OperationException(name+" is constant and can't be re-assigned");
    }
    else {
      attributes.put(name, valueAddr);
      attrModifiers.put(name, modifiers.size() == 0 ? EMPTY_ATTRMODS : EnumSet.copyOf(modifiers));
    }
  }

  public void setAttrModifers(String name, AttrModifier ... modifiers) throws OperationException {
    setAttrModifers(name, Arrays.asList(modifiers));
  }

  public void setAttrModifers(String name, Collection<AttrModifier> modifiers) throws OperationException {
    if (isSealed) {
      throw new OperationException("The object is sealed and immutable");
    }
    else if(!attrModifiers.containsKey(name)) {
      throw new OperationException(name+" doesn't exist on this object");
    }
    else {
      attrModifiers.put(name, modifiers.size() == 0 ? EnumSet.noneOf(AttrModifier.class) : EnumSet.copyOf(modifiers));
    }
  }

  public void appendAttrModifier(String name, AttrModifier modifier) throws OperationException {
    if (isSealed) {
      throw new OperationException("The object is sealed and immutable");
    }
    else if(!attrModifiers.containsKey(name)) {
      throw new OperationException(name+" doesn't exist on this object");
    }
    else {
      EnumSet<AttrModifier> curMods = attrModifiers.get(name);
      curMods.add(modifier);
    }
  }

  public RuntimeInstance $add(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    throw new OperationException("+ is not defined for this object.");
  }

  public RuntimeInstance $inc(HeapAllocator alloc) throws OperationException {
    throw new OperationException("+ is not defined for this object.");
  }

  public RuntimeInstance $dec(HeapAllocator alloc) throws OperationException {
    throw new OperationException("+ is not defined for this object.");
  }
  
  public RuntimeInstance getAttr(String name) {
    long start = System.nanoTime();
    final RuntimeInstance instance = attributes.get(name);
    long end = System.nanoTime();

    GeneralMetrics.addTimes(Meaures.ATTR_LOOKUP, end - start);

    return instance;
  }

  public Set<AttrModifier> attrModifiers(String name) {
    return attrModifiers.get(name);
  }

  public Set<String> attrs() {
    return attributes.keySet();
  }

  public boolean is(String name, AttrModifier mod) {
    return hasAttr(name) ? attrModifiers.get(name).contains(mod) : false;
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
  }
    
  public Map<String, RuntimeInstance> getAttributes() {
    return attributes;
  }

  @Override
  public String toString() {
    String x = System.lineSeparator()+"================================"+System.lineSeparator();
    for (Entry<String, RuntimeInstance> a : attributes.entrySet()) {
      x += " => "+(a.getValue() == this)+" "+a.getKey()+" = "+ System.lineSeparator();
    }
    return x;
  }
  
  /*
  @Override
  public void finalize(){
     System.out.println(" Being colltect: "+this);
  }
  */
}
