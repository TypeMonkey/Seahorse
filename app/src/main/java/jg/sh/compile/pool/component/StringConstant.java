package jg.sh.compile.pool.component;

public class StringConstant extends PoolComponent {

  private final String value;
  
  public StringConstant(String value) {
    super(ComponentType.STRING);
    this.value = value;
  }
  
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return "<string> "+value;
  }
}
