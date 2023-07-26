package jg.sh.compile.pool.component;

public class BoolConstant extends PoolComponent{

  private final boolean value;
  
  public BoolConstant(boolean value) {
    super(ComponentType.BOOLEAN);
    this.value = value;
  }
  
  public boolean getValue() {
    return value;
  }
  
  @Override
  public String toString() {
    return "<bool> "+value;
  }
  
}

