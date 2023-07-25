package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class StoreInstr extends Instruction implements MutableInstr {
  
 private int index;
  
  public StoreInstr(Location start, Location end, OpCode initialOpCode, int index) {
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
