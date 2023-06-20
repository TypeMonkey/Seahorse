package jg.sh.interpret.objects;

import java.util.ArrayList;
import java.util.List;

import jg.sh.interpret.TypeConstants;

public class ArrayInstance extends Instance{
  
  private List<Integer> array; //addresses of element
  private int size;
  
  public ArrayInstance() {
    super(TypeConstants.ARRAY_TYPE_CODE);
    this.array = new ArrayList<>();
    this.size = 0;
  }

  public void addElement(int address) {
    array.add(address);
    size++;
  }
  
  public int removeElement(int index) throws IndexOutOfBoundsException {
    return array.remove(index);
  }
  
  public int setElement(int index, int newValue) throws IndexOutOfBoundsException {
    return array.set(index, newValue);
  }
  
  public int getElement(int index) throws IndexOutOfBoundsException {
    return array.get(index);
  }
  
  public int getSize() {
    return size;
  }
  
  @Override
  public String toString() {
    return "<array> "+size+" | "+array;
  }

}
