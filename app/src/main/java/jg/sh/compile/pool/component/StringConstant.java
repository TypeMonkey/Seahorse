package jg.sh.compile.pool.component;

public class StringConstant implements PoolComponent {

  private final String value;
  
  public StringConstant(String value) {
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }
  
  @Override
  public ComponentType getType() {
    return ComponentType.STRING;
  }

  @Override
  public String toString() {
    return "<string> "+value;
  }
}
