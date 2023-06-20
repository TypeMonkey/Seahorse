package jg.sh.interpret.objects;

import jg.sh.interpret.TypeConstants;

public class IntInstance extends Instance{

  private final long value;
  
  public IntInstance(long value) {
    super(TypeConstants.INT_TYPE_CODE);
    this.value = value;
  }

  public long getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "<int> "+value;
  }

}
