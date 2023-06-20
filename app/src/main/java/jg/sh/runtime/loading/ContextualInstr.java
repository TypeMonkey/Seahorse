package jg.sh.runtime.loading;

import jg.sh.irgen.instrs.Instruction;

public class ContextualInstr {
  private final Instruction instr;
  private final int exceptionJumpIndex;
  
  public ContextualInstr(Instruction instr, int exceptionJumpIndex) {
    this.instr = instr;
    this.exceptionJumpIndex = exceptionJumpIndex;
  }
  
  public Instruction getInstr() {
    return instr;
  }
  
  public int getExceptionJumpIndex() {
    return exceptionJumpIndex;
  }
  
  @Override
  public String toString() {
    return "("+exceptionJumpIndex+")   "+instr;
  }
}