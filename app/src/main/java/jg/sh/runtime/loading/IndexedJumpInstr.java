package jg.sh.runtime.loading;

import jg.sh.irgen.instrs.Instruction;
import jg.sh.irgen.instrs.JumpInstr;

public class IndexedJumpInstr extends Instruction{
  
  private final int jumpIndex;

  public IndexedJumpInstr(JumpInstr instr, int jumpIndex) {
    super(instr.getLine(), instr.getCol(), instr.getOpCode());
    
    this.jumpIndex = jumpIndex;
  }

  public int getJumpIndex() {
    return jumpIndex;
  }

  @Override
  public String toString() {
    return getOpCode().name().toLowerCase()+" "+jumpIndex;
  }
}
