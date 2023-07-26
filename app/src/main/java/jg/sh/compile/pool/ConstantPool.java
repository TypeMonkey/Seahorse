package jg.sh.compile.pool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.MutableInstr;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.CodeObject;
import jg.sh.compile.pool.component.DataRecord;
import jg.sh.compile.pool.component.ErrorHandlingRecord;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.pool.component.StringConstant;

public class ConstantPool {

  public static class MutableIndex {

    private final List<MutableInstr> linkedLoads;
    private int index;

    public MutableIndex(int initialIndex) {
      this.linkedLoads = new ArrayList<>();
      this.index = initialIndex;
    }

    public MutableIndex() {
      this(-1);
    }

    public <T extends MutableInstr> T linkInstr(T mutableInstr) {
      mutableInstr.setIndex(index);
      linkedLoads.add(mutableInstr);
      return mutableInstr;
    }

    public void increment() {
      setIndex(index + 1);
    }

    public void decrement() {
      setIndex(index - 1);
    }

    public void setIndex(int index) {
      this.index = index;

      for (MutableInstr instr : linkedLoads) {
        instr.setIndex(index);
      }
    }

    public int getIndex() {
      return index;
    }
  }
  
  private final Map<String, StringConstant> stringConstants;
  private final Map<Long, IntegerConstant> integerConstants;
  private final Map<Double, FloatConstant> floatConstants;
  private final Map<Boolean, BoolConstant> booleanConstants;
  private final Map<String, DataRecord> dataRecords;
  private final List<ErrorHandlingRecord> errorHandlingRecords;

  private final List<PoolComponent> allComponents;
  
  private int currentIndex;
  
  public ConstantPool() {
    this.stringConstants = new HashMap<>();
    this.integerConstants = new HashMap<>();
    this.floatConstants = new HashMap<>();
    this.booleanConstants = new HashMap<>();
    this.dataRecords = new HashMap<>();
    this.errorHandlingRecords = new ArrayList<>();
    
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
    addBoolean(true);   
    addBoolean(false);

    //Load integer values from -255 to 255
    for(long i = -255; i <= 255; i++) {
      addInt(i);
    }
  }

  public StringConstant addString(String value) {
    return stringConstants.computeIfAbsent(value, 
                          (e) -> {
                            return addNewComponent(new StringConstant(e));
                          });
  }

  public IntegerConstant addInt(long value) {
    return integerConstants.computeIfAbsent(value, 
                          (e) -> {
                            return addNewComponent(new IntegerConstant(e));
                          });
  }

  public FloatConstant addFloat(double value) {
    return floatConstants.computeIfAbsent(value, 
                          (e) -> {
                            return addNewComponent(new FloatConstant(e));
                          });
  }

  public BoolConstant addBoolean(boolean value) {
    return booleanConstants.computeIfAbsent(value, 
                          (e) -> {
                            return addNewComponent(new BoolConstant(e));
                          });
  }

  public CodeObject addCodeObject(FunctionSignature signature, 
                                 String boundName, 
                                 Map<String, Integer> keywordIndexes, 
                                 int varArgIndex,
                                 int keywordVarArgIndex,
                                 List<Instruction> instrs, 
                                 int [] captures) {
    final CodeObject codeObject = new CodeObject(signature, boundName, keywordIndexes, varArgIndex, keywordVarArgIndex, instrs, captures);
    return addNewComponent(codeObject);
  }

  public ErrorHandlingRecord addErrorHandling(String startTryLabel, 
                                    String endTryLabel, 
                                    String catchLabel) {
    final ErrorHandlingRecord record = new ErrorHandlingRecord(startTryLabel, endTryLabel, catchLabel);
    errorHandlingRecords.add(record);
    return addNewComponent(record);
  }

  public DataRecord addDataRecord(String name, 
                                 FunctionSignature constructorSignature, 
                                 LinkedHashMap<String, MutableIndex> methods,
                                 boolean isSealed) {
    final DataRecord record = new DataRecord(name, constructorSignature, methods, isSealed);
    return dataRecords.computeIfAbsent(record.getName(), 
                          (e) -> {
                            return addNewComponent(record);
                          });
  }
  
  private <T extends PoolComponent> T addNewComponent(T component) {
    allComponents.add(component);
    component.setIndex(new MutableIndex(currentIndex++));
    return component;
  }

  public void removeComponent(int index) {
    allComponents.set(index, null);
  }

  public void squash() {
    for (int i = allComponents.size() - 1; i >= 0; i--) {
      if (allComponents.get(i) == null) {
        allComponents.remove(i);

        for (int t = allComponents.size() - 1; t >= i; t--) {
          allComponents.get(t).getIndex().decrement();
        }
      }
    }
  }
  
  public PoolComponent getComponent(int index) {
    return allComponents.get(index);
  }
  
  public List<PoolComponent> getMembers() {
    return allComponents;
  }
  
  public Map<String, StringConstant> getStringConstants() {
    return stringConstants;
  }
  
  public Map<Boolean, BoolConstant> getBooleanConstants() {
    return booleanConstants;
  }
  
  public Map<Long, IntegerConstant> getIntegerConstants() {
    return integerConstants;
  }
  
  public Map<Double, FloatConstant> getFloatConstants() {
    return floatConstants;
  }
  
  public List<ErrorHandlingRecord> getErrorHandlingRecords() {
    return errorHandlingRecords;
  }

  public int getPoolSize() {
    return allComponents.size();
  }
}
