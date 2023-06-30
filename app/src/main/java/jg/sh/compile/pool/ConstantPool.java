package jg.sh.compile.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.ErrorHandlingRecord;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.pool.component.StringConstant;

public class ConstantPool {
  
  private final Map<String, Integer> stringConstants;
  private final Map<Long, Integer> integerConstants;
  private final Map<Double, Integer> floatConstants;
  private final Map<Boolean, Integer> booleanConstants;
  private final List<ErrorHandlingRecord> errorHandlingRecords;
  
  private final List<PoolComponent> allComponents;
  
  private int currentIndex;
  
  public ConstantPool() {
    this.stringConstants = new HashMap<>();
    this.integerConstants = new HashMap<>();
    this.floatConstants = new HashMap<>();
    this.booleanConstants = new HashMap<>();
    
    errorHandlingRecords = new ArrayList<>();
    allComponents = new ArrayList<>();
    currentIndex = 0;
  }
  
  /**
   * Loads a few common constants a module may use.
   * 
   * Notably, it'll add the following constants:
   *  - the boolean values: true and false
   *  - integer values within the inclusive range of [ -255 to 255 ]
   *  
   *  It's optional to invoke this method, but recommended to do so
   *  as it may reduce additional allocations of constants
   */
  public void preloadCommonConstants() {
    //Load boolean values
    addComponent(new BoolConstant(true));   
    addComponent(new BoolConstant(false));

    //Load integer values from -255 to 255
    for(long i = -255; i <= 255; i++) {
      addNewComponent(new IntegerConstant(i));
    }
  }
  
  /**
   * Adds a constant to this ContantPool
   * @param component - the PoolComponent (constant) to add to the pool
   *   
   *   If the component is a string, boolean, integer, or float, this method
   *   will first check is an equivalent value has already been added to this pool.
   *   
   *   If so, it'll return that value's index. Else, it'll add the component to the pool
   *   and return the index associated with the addition.
   * 
   * @return the index to refer to the given constant
   */
  public int addComponent(PoolComponent component) { 
    switch (component.getType()) {
      case STRING: {
        StringConstant stringConstant = (StringConstant) component;
        if(stringConstants.containsKey(stringConstant.getValue())) {
          return stringConstants.get(stringConstant.getValue());
        }
        else {
          int index = addNewComponent(component);
          stringConstants.put(stringConstant.getValue(), index);
          return index;
        }
      }
      case BOOLEAN: {
        BoolConstant boolConstant = (BoolConstant) component;
        if(booleanConstants.containsKey(boolConstant.getValue())) {
          return booleanConstants.get(boolConstant.getValue());
        }
        else {
          int index = addNewComponent(component);
          booleanConstants.put(boolConstant.getValue(), index);
          return index;
        }
      }
      case INT: {
        IntegerConstant integerConstant = (IntegerConstant) component;
        if(integerConstants.containsKey(integerConstant.getValue())) {
          return integerConstants.get(integerConstant.getValue());
        }
        else {
          int index = addNewComponent(component);
          integerConstants.put(integerConstant.getValue(), index);
          return index;
        }
      }
      case FLOAT: {
        FloatConstant floatConstant = (FloatConstant) component;
        if(floatConstants.containsKey(floatConstant.getValue())) {
          return floatConstants.get(floatConstant.getValue());
        }
        else {
          int index = addNewComponent(component);
          floatConstants.put(floatConstant.getValue(), index);
          return index;
        }
      }
      case ERROR_RECORD: {
        ErrorHandlingRecord errorHandlingRecord = (ErrorHandlingRecord) component;
        errorHandlingRecords.add(errorHandlingRecord);
        return addNewComponent(errorHandlingRecord);
      }
      default:
        return addNewComponent(component);
    }
  }
  
  private int addNewComponent(PoolComponent component) {
    allComponents.add(component);
    return currentIndex++;
  }
  
  public PoolComponent getComponent(int index) {
    return allComponents.get(index);
  }
  
  public List<PoolComponent> getMembers() {
    return allComponents;
  }
  
  public Map<String, Integer> getStringConstants() {
    return stringConstants;
  }
  
  public Map<Boolean, Integer> getBooleanConstants() {
    return booleanConstants;
  }
  
  public Map<Long, Integer> getIntegerConstants() {
    return integerConstants;
  }
  
  public Map<Double, Integer> getFloatConstants() {
    return floatConstants;
  }
  
  public List<ErrorHandlingRecord> getErrorHandlingRecords() {
    return errorHandlingRecords;
  }
  
  public int getPoolSize() {
    return allComponents.size();
  }
}
