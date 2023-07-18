package jg.sh.runtime.objects.literals;

public class RuntimeInteger extends RuntimePrimitive {

  private final long value;
  
  public RuntimeInteger(long value) {
    super();
    this.value = value;
  }

  public long getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
