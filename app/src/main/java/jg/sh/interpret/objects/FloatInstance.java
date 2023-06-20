package jg.sh.interpret.objects;

import jg.sh.interpret.TypeConstants;

public class FloatInstance extends Instance{
  
  private final double value;

  public FloatInstance(double value) {
    super(TypeConstants.FLOAT_TYPE_CODE);
    this.value = value;
  }
  
  public double getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "<float> '"+value+"'";
  }

}
