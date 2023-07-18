package jg.sh.compile.pool.component;

public class IntegerConstant implements PoolComponent{

  private final long value;
  
  public IntegerConstant(long value) {
    this.value = value;
  }
  
  public long getValue() {
    return value;
  }
  
  @Override
  public ComponentType getType() {
    return ComponentType.INT;
  }
  
  @Override
  public String toString() {
    return "<int> "+value;
  }
  
}
