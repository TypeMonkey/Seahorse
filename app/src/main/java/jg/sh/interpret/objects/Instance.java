package jg.sh.interpret.objects;

public abstract class Instance {

  private final int typeCode;
  
  public Instance(int typeCode) {
    this.typeCode = typeCode;
  }
  
  public int getTypeCode() {
    return typeCode;
  }
  
  public abstract String toString();
}
