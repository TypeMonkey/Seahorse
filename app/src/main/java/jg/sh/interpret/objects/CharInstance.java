package jg.sh.interpret.objects;

import jg.sh.interpret.TypeConstants;

public class CharInstance extends Instance{
  
  private final char value;

  public CharInstance(char value) {
    super(TypeConstants.CHAR_TYPE_CODE);
    this.value = value;
  }
  
  public char getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "<char> '"+value+"'";
  }

}
