package jg.sh.compile.optimization;

import jg.sh.compile.pool.component.PoolComponent;

public class OptimizableTarget {

  private final PoolComponent component;

  public OptimizableTarget(PoolComponent component) {
    this.component = component;
  }
  
  public PoolComponent getComponent() {
    return component;
  }
}
