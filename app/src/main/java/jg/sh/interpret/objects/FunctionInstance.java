package jg.sh.interpret.objects;

import jg.sh.interpret.TypeConstants;
import jg.sh.interpret.components.Callable;

public class FunctionInstance extends Instance{

  private final Callable callable;
  
  public FunctionInstance(Callable callable) {
    super(TypeConstants.FUNC_TYPE_CODE);
    this.callable = callable;
  }
  
  public Callable getCallable() {
    return callable;
  }

  @Override
  public String toString() {
    return "<func>";
  }
}
