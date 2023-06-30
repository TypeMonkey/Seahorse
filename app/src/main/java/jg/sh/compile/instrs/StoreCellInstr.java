package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class StoreCellInstr extends Instruction{
  
 private int index;
  
  public StoreCellInstr(Location start, Location end, OpCode initialOpCode, int index) {
    super(start, end, initialOpCode);
    this.index = index;
  }

  public void setIndex(int index) {
    this.index = index;
  }
  
  public void setOpCode(OpCode opCode) {
    this.opCode = opCode;
  }
  
  public int getIndex() {
    return index;
  }
  
  @Override
  public String toString() {
    return opCode.name().toLowerCase()+" "+index;
  }
}
