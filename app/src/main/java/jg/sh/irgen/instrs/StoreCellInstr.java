package jg.sh.irgen.instrs;

public class StoreCellInstr extends Instruction{
  
 private int index;
  
  public StoreCellInstr(int line, int col, OpCode initialOpCode, int index) {
    super(line, col, initialOpCode);
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
