package jg.sh.compile.instrs;

import jg.sh.common.Location;

/**
 * Specifies a LOAD_XXX instruction.
 * 
 * @author Jose
 */
public class LoadInstr extends Instruction implements MutableInstr {

  private int index;
  
  public LoadInstr(Location start, Location end, OpCode initialOpCode, int index) {
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
