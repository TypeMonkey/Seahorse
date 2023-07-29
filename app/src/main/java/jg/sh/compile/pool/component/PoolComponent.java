package jg.sh.compile.pool.component;

import jg.sh.compile.instrs.MutableIndex;
import jg.sh.compile.instrs.MutableInstr;

public abstract class PoolComponent {
  
  public enum ComponentType{
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    ERROR_RECORD,
    DATA_RECORD,
    CODE
  }

  private final ComponentType type;

  private MutableIndex index;

  public PoolComponent(ComponentType type) {
    this.type = type;
  }

  public void setIndex(MutableIndex index) {
    this.index = index;
  }
   
  public MutableIndex getIndex() {
    return index;
  }

  public int getExactIndex() {
    return index.getIndex();
  }

  public ComponentType getType() {
    return type;
  }
  
  @Override
  public abstract String toString();

}
