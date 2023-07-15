package jg.sh.runtime.alloc;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.loading.ContextualInstr;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.Initializer;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;

public class HeapAllocator {
    
  private static final RuntimeBool TRUE = new RuntimeBool(true);
  private static final RuntimeBool FALSE = new RuntimeBool(false);
  private static final RuntimeInteger[] SMALL_NUMS = new RuntimeInteger[256];
  
  static {
    for (int i = 0; i < SMALL_NUMS.length; i++) {
      SMALL_NUMS[i] = new RuntimeInteger(i);
    }
  }
    
  //private List<WeakReference<RuntimeInstance>> storage;
  private int storageLimit;
  private int heapPointer;
  
  public HeapAllocator(int storageLimit) {
    //this.storage = new ArrayList<>();    
    this.storageLimit = storageLimit;
    this.heapPointer = 0;
  }
  
  public void setHeapPointer(int heapPointer) {
    this.heapPointer = heapPointer;
  }
  
  public RuntimeError allocateError(String msg) {    
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeError object = new RuntimeError(allocateString(msg));
    //storage.add(new WeakReference<>(object));
    heapPointer++;
    
    return object;
  }

  public RuntimeInstance allocateEmptyObject() {    
    return allocateEmptyObject(null);
  }
  
  public RuntimeInstance allocateEmptyObject(BiConsumer<Initializer, RuntimeInstance> initializer) {    
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeInstance object = new RuntimeInstance(initializer);
    ///storage.add(new WeakReference<>(object));
    heapPointer++;
    
    return object;
  }
  
  public RuntimeArray allocateEmptyArray() {    
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeArray array = new RuntimeArray();
    //storage.add(new WeakReference<>(array));
    heapPointer++;
    
    return array;
  }

  
  public RuntimeInteger allocateInt(long value) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    if (value >= 0 && value < SMALL_NUMS.length) {
      return SMALL_NUMS[(int) value];
    }
    
    final RuntimeInteger integer = new RuntimeInteger(value);
    //storage.add(new WeakReference<>(integer));
    heapPointer++;
    
    return integer;
  }

  public RuntimeBool allocateBool(boolean value) {
    //perform garbage collection prior to allocation    
    return value ? TRUE : FALSE;
  }

  public RuntimeFloat allocateFloat(double value) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeFloat floatValue = new RuntimeFloat(value);
    //storage.add(new WeakReference<>(floatValue));
    heapPointer++;
    
    return floatValue;
  }

  public RuntimeString allocateString(String value) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeString str = new RuntimeString(value);
    //storage.add(new WeakReference<>(str));
    heapPointer++;
    
    return str;
  }
  
  public RuntimeCodeObject allocateCodeObject(String boundName, FunctionSignature signature,  Map<String, Integer> keywordIndexes, ContextualInstr [] instrs, int [] captures) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeCodeObject codeObject = new RuntimeCodeObject(boundName, signature, keywordIndexes, instrs, captures);
    //storage.add(new WeakReference<>(codeObject));
    heapPointer++;
    
    return codeObject;
  }
  
  
  public RuntimeCallable allocateCallable(RuntimeModule hostModule, RuntimeInstance self, RuntimeCodeObject codeObject) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    
    final RuntimeCallable callable = new RuntimeCallable(hostModule, self, codeObject);
    //storage.add(new WeakReference<>(callable));
    heapPointer++;
    
    return callable;
  }
  
  public RuntimeCallable allocateCallable(RuntimeModule hostModule, RuntimeInstance self, RuntimeCodeObject codeObject, CellReference [] captures) {
    //perform garbage collection prior to allocation
    if (heapPointer >= storageLimit) {
      throw new IllegalStateException("Out of memory when allocating integer!");
    }
    
    final RuntimeCallable callable = new RuntimeCallable(hostModule, self, codeObject, captures);
    //storage.add(new WeakReference<>(callable));
    heapPointer++;
    
    return callable;
  }
  
  /*
  public RuntimeInstance getInstance(int address) {
    return storage.get(address).get();
  }
  */
  
  public int getHeapPointer() {
    return heapPointer;
  }
  
  /*
  public List<WeakReference<RuntimeInstance>> getStorage() {
    return storage;
  }
  */
}
