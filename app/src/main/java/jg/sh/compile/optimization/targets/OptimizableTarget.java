package jg.sh.compile.optimization.targets;

import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

public abstract class OptimizableTarget<T> {

  private final T value;
  private PoolComponent component;

  public OptimizableTarget(T value) {
    this.value = value;
  }

  public PoolComponent getPoolComponent(ConstantPool pool) {
    if (component == null) {
      component = allocateAsComponent(pool);
    }

    return component;
  }
  
  protected abstract PoolComponent allocateAsComponent(ConstantPool pool);

  public T getValue() {
    return value;
  }
}
