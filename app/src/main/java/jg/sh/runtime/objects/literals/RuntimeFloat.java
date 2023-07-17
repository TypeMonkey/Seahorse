package jg.sh.runtime.objects.literals;

public final class RuntimeFloat extends RuntimePrimitive {

  private final double value;
  
  public RuntimeFloat(double value) {
    super();
    this.value = value;
  }
  
  public double getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return String.valueOf(value);
  }
}