package jg.sh.compile.pool.component;

import java.util.LinkedHashMap;

import jg.sh.common.FunctionSignature;

public class DataRecord implements PoolComponent {

  private final String name;
  private final FunctionSignature constructorSignature;
  private final LinkedHashMap<String, Integer> methods;
  private final boolean isSealed;

  /**
   * Cosntructs a DataRecord
   * @param name - name of this DataRecord
   * @param methods - the methods (function bound to instances of this DataRecord) for this DataRecord
   * @param isSealed - whether this DataRecord is sealed.
   */
  public DataRecord(String name, 
                    FunctionSignature constructorSignature, 
                    LinkedHashMap<String, Integer> methods,
                    boolean isSealed) {
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

  public LinkedHashMap<String, Integer> getMethods() {
    return methods;
  }

  public boolean isSealed() {
    return isSealed;
  }

  @Override
  public ComponentType getType() {
    return ComponentType.DATA_RECORD;
  }
}
