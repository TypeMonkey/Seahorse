package jg.sh.irgen.pool.component;

public class BoolConstant implements PoolComponent{

  private final boolean value;
  
  public BoolConstant(boolean value) {
    this.value = value;
  }
  
  public boolean getValue() {
    return value;
  }
  
  @Override
  public ComponentType getType() {
    return ComponentType.BOOLEAN;
  }
  
  @Override
  public String toString() {
    return "<bool> "+value;
  }
  
}

