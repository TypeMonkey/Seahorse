package jg.sh.compile.pool.component;

import jg.sh.common.Location;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.MutableInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool.MutableIndex;

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

  public <T extends MutableInstr> T linkInstr(T instr) {
    return index.linkInstr(instr);
  }
  
  @Override
  public abstract String toString();

}
