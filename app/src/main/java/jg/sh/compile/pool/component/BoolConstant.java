package jg.sh.compile.pool.component;

import jg.sh.common.Location;

public class BoolConstant extends PoolComponent{

  private final boolean value;
  
  public BoolConstant(boolean value) {
    super(ComponentType.BOOLEAN);
    this.value = value;
  }
  
  public boolean getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "<bool> "+value;
  }
  
}

