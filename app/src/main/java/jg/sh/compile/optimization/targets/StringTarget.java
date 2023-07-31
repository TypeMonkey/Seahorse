package jg.sh.compile.optimization.targets;

import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

public class StringTarget extends OptimizableTarget<String> {

  public StringTarget(String value) {
    super(value);
  }

  @Override
  public PoolComponent allocateAsComponent(ConstantPool pool) {
    return pool.addString(getValue());
  }
}
