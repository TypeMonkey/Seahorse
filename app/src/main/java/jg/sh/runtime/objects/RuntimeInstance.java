package jg.sh.runtime.objects;

import java.util.Arrays;
import java.util.Collection;
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
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.literals.FuncOperatorCoupling;
import jg.sh.runtime.objects.literals.RuntimeBool;

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
    else {
      final EnumSet<AttrModifier> curMods = attrModifiers.get(name);
      if (curMods == null) {
        throw new OperationException(name+" doesn't exist on this object");
      }
      curMods.add(modifier);
    }
  }

  public Callable $call() throws OperationException {
    final RuntimeInstance callable = attributes.get("$call");
    if (callable == null || !(callable instanceof Callable)) {
      throw new OperationException("object isn't callable");
    }
    
    return (Callable) callable;
  }

  public RuntimeInstance $equal(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    return alloc.allocateBool(this == otherOperand || equals(otherOperand));
  }

  public RuntimeInstance $add(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance addFunc = attributes.get(FuncOperatorCoupling.ADD.getFuncName());
    if (addFunc != null) {
      return addFunc;
    }
    throw new OperationException("+ is not defined for this object.");
  }

  public RuntimeInstance $sub(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance subFunc = attributes.get(FuncOperatorCoupling.SUB.getFuncName());
    if (subFunc != null) {
      return subFunc;
    }
    throw new OperationException("- is not defined for this object.");
  }

  public RuntimeInstance $mul(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance mulFunc = attributes.get(FuncOperatorCoupling.MUL.getFuncName());
    if (mulFunc != null) {
      return mulFunc;
    }
    throw new OperationException("* is not defined for this object.");
  }

  public RuntimeInstance $div(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance divFunc = attributes.get(FuncOperatorCoupling.DIV.getFuncName());
    if (divFunc != null) {
      return divFunc;
    }
    throw new OperationException("/ is not defined for this object.");
  }

  public RuntimeInstance $mod(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance modFunc = attributes.get(FuncOperatorCoupling.MOD.getFuncName());
    if (modFunc != null) {
      return modFunc;
    }
    throw new OperationException("% is not defined for this object.");
  }

  public RuntimeInstance $neg(HeapAllocator alloc) throws OperationException {
    final RuntimeInstance negFunc = attributes.get(FuncOperatorCoupling.NEG.getFuncName());
    if (negFunc != null) {
      return negFunc;
    }
    throw new OperationException("negation (-) is not defined for this object.");
  }

  public RuntimeInstance $not(HeapAllocator alloc) throws OperationException {
    final RuntimeInstance notFunc = attributes.get(FuncOperatorCoupling.NOT.getFuncName());
    if (notFunc != null) {
      return notFunc;
    }
    throw new OperationException("negation (!) is not defined for this object.");
  }

  public RuntimeInstance $less(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance lessFunc = attributes.get(FuncOperatorCoupling.LESS.getFuncName());
    if (lessFunc != null) {
      return lessFunc;
    }
    throw new OperationException("< is not defined for this object.");
  }

  public RuntimeInstance $great(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance greatFunc = attributes.get(FuncOperatorCoupling.GREAT.getFuncName());
    if (greatFunc != null) {
      return greatFunc;
    }
    throw new OperationException("> is not defined for this object.");
  }

  public RuntimeInstance $lesse(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance lessEqFunc = attributes.get(FuncOperatorCoupling.LESSE.getFuncName());
    if (lessEqFunc != null) {
      return lessEqFunc;
    }
    throw new OperationException("<= is not defined for this object.");
  }

  public RuntimeInstance $greate(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance greatEqFunc = attributes.get(FuncOperatorCoupling.GREATE.getFuncName());
    if (greatEqFunc != null) {
      return greatEqFunc;
    }
    throw new OperationException(">= is not defined for this object.");
  }

  public RuntimeInstance $band(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance bandFunc = attributes.get(FuncOperatorCoupling.BAND.getFuncName());
    if (bandFunc != null) {
      return bandFunc;
    }
    throw new OperationException("& is not defined for this object.");
  }

  public RuntimeInstance $bor(RuntimeInstance otherOperand, HeapAllocator alloc) throws OperationException {
    final RuntimeInstance borFunc = attributes.get(FuncOperatorCoupling.BOR.getFuncName());
    if (borFunc != null) {
      return borFunc;
    }
    throw new OperationException("| is not defined for this object.");
  }

  public RuntimeInstance $inc(HeapAllocator alloc) throws OperationException {
    throw new OperationException("increment is not defined for this object.");
  }

  public RuntimeInstance $dec(HeapAllocator alloc) throws OperationException {
    throw new OperationException("decrement is not defined for this object.");
  }

  public RuntimeInstance $getAtIndex(RuntimeInstance index, HeapAllocator alloc) throws OperationException {
    final String attrName = index.toString();
    return getAttr(attrName);
  }

  public void $setAtIndex(RuntimeInstance index, RuntimeInstance value, HeapAllocator alloc) throws OperationException {
    final String attrName = index.toString();
    setAttribute(attrName, value);
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

  public boolean hasAttrs() {
    return !attributes.isEmpty();
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
      x += " => "+(a.getValue() == this)+" "+a.getKey()+" = "+a.getValue()+ System.lineSeparator();
    }
    return x;
  }
}
