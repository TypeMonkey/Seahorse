package jg.sh.compile.optimization.targets;

import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

public class BooleanTarget extends OptimizableTarget<Boolean> {

  public BooleanTarget(boolean value) {
    super(value);
  }

  @Override
  public PoolComponent allocateAsComponent(ConstantPool pool) {
    return pool.addBoolean(getValue());
  }
}
