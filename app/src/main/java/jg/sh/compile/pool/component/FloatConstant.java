package jg.sh.compile.pool.component;

import jg.sh.common.Location;
import jg.sh.compile.pool.ConstantPool.MutableIndex;

public class FloatConstant extends PoolComponent {
  
  private final double value;
  
  public FloatConstant(double value) {
    super(ComponentType.FLOAT);
    this.value = value;
  }
  
  public double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "<float> "+value;
  }
}
