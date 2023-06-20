package jg.sh.interpret.alloc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.interpret.objects.ArrayInstance;
import jg.sh.interpret.objects.CharInstance;
import jg.sh.interpret.objects.ClassInstance;
import jg.sh.interpret.objects.DictInstance;
import jg.sh.interpret.objects.FloatInstance;
import jg.sh.interpret.objects.Instance;
import jg.sh.interpret.objects.IntInstance;

public class Heap {
  
  private final List<Instance> heap;
  
  private int heapSize;
  
  public Heap() {
    this.heap = new ArrayList<>();
    this.heapSize = 0;
  }
  
  public int getAttr(int address, String attrName) {
    ClassInstance instance = (ClassInstance) heap.get(address);
    return instance.getAttr(attrName);
  }
  
  public int setAttr(int address, String attrName, int newValue) {
    ClassInstance instance = (ClassInstance) heap.get(address);
    int oldValue = instance.setAttr(attrName, newValue);
    
    return oldValue;
  }
  
  public int getValueAtIndex(int address, int index) {
    ArrayInstance instance = (ArrayInstance) heap.get(address);
    return instance.getElement(index);
  }
  
  public int setValueAtIndex(int address, int index, int newValue) {
    ArrayInstance instance = (ArrayInstance) heap.get(address);
    int oldValue = instance.setElement(index, newValue);
    
    return oldValue;
  }
  
  public int alloc(int typeCode) {
    ClassInstance classInstance = new ClassInstance(typeCode);
    heap.add(classInstance);
    heapSize++;
    
    return heapSize - 1;
  }
  
  public int allocDict(Map<String, Integer> dict) {
    DictInstance dictInstance = new DictInstance();
    
    for(Entry<String, Integer> dictEntry : dict.entrySet()) {
      dictInstance.setAttr(dictEntry.getKey(), dictEntry.getValue());
    }
    
    return addInstance(dictInstance);
  }
  
  public int allocArray(int [] arrValues) {
    ArrayInstance arrayInstance = new ArrayInstance();
    
    for (int i : arrValues) {
      arrayInstance.addElement(i);
    }
    
    return addInstance(arrayInstance);
  }
  
  public int allocInt(long value) {
    IntInstance instance = new IntInstance(value);
    return addInstance(instance);
  }
  
  public int allocChar(char value) {
    CharInstance instance = new CharInstance(value);
    return addInstance(instance);
  }
  
  public int allocFloat(double value) {
    FloatInstance instance = new FloatInstance(value);
    return addInstance(instance);
  }
  
  private int addInstance(Instance instance) {
    heap.add(instance);
    heapSize++;
    
    return heapSize - 1;
  }
}
