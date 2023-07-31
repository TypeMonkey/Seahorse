package jg.sh.compile.optimization.targets;

import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

public class FloatTarget extends OptimizableTarget<Double> {

  public FloatTarget(double value) {
    super(value);
  }

  @Override
  public PoolComponent allocateAsComponent(ConstantPool pool) {
    return pool.addFloat(getValue());
  }
}
