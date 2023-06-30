package jg.sh.compile.pool.component;

public class FloatConstant implements PoolComponent {
  
  private final double value;
  
  public FloatConstant(double value) {
    this.value = value;
  }
  
  public double getValue() {
    return value;
  }

  @Override
  public ComponentType getType() {
    return ComponentType.FLOAT;
  }

  @Override
  public String toString() {
    return "<float> "+value;
  }
}
