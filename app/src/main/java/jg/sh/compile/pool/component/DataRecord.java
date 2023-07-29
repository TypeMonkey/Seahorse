package jg.sh.compile.pool.component;

import java.util.LinkedHashMap;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.instrs.MutableIndex;

public class DataRecord extends PoolComponent {

  private final String name;
  private final FunctionSignature constructorSignature;
  private final LinkedHashMap<String, MutableIndex> methods;
  private final boolean isSealed;

  /**
   * Cosntructs a DataRecord
   * @param name - name of this DataRecord
   * @param methods - the methods (function bound to instances of this DataRecord) for this DataRecord
   * @param isSealed - whether this DataRecord is sealed.
   */
  public DataRecord(String name, 
                    FunctionSignature constructorSignature, 
                    LinkedHashMap<String, MutableIndex> methods,
                    boolean isSealed) {
    super(ComponentType.DATA_RECORD);
    this.name = name;
    this.constructorSignature = constructorSignature;
    this.methods = methods;
    this.isSealed = isSealed;
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof DataRecord && ((DataRecord) o).name.equals(name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  public String getName() {
    return name;
  }

  public FunctionSignature getConstrSignature() {
    return constructorSignature;
  }

  public LinkedHashMap<String, MutableIndex> getMethods() {
    return methods;
  }

  public boolean isSealed() {
    return isSealed;
  }

  @Override
  public String toString() {
    return "<data_record> "+name+" methods: "+methods;
  }
}
