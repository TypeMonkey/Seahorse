package jg.sh.compile.optimization.targets;

import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

public class IntegerTarget extends OptimizableTarget<Long> {
  
  public IntegerTarget(long value) {
    super(value);
  }

  @Override
  public PoolComponent allocateAsComponent(ConstantPool pool) {
    return pool.addInt(getValue());
  }
}
