package jg.sh.compile.results;

import java.util.Arrays;
import java.util.Collections;

import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.pool.component.PoolComponent;

public class ConstantResult extends NodeResult {

  private final PoolComponent constant;

  public ConstantResult(PoolComponent constant, LoadInstr loadInstr) {
    super(Collections.emptyList(), Arrays.asList(loadInstr));
    this.constant = constant;
  }
  
  public PoolComponent getConstant() {
    return constant;
  }
}