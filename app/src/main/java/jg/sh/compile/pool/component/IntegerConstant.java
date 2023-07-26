package jg.sh.compile.pool.component;

public class IntegerConstant extends PoolComponent {

  private final long value;
  
  public IntegerConstant(long value) {
    super(ComponentType.INT);
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
