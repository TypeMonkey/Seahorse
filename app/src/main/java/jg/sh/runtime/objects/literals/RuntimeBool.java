package jg.sh.runtime.objects.literals;

public final class RuntimeBool extends RuntimePrimitive {
  
  private final boolean value;
  
  public RuntimeBool(boolean value) {
    super();
    this.value = value;
  }
  
  public boolean getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }
}
