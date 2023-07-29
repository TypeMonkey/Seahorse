package jg.sh.runtime.instrs;

import jg.sh.compile.instrs.OpCode;

public class NoArgInstruction extends RuntimeInstruction {

  public NoArgInstruction(OpCode opCode, int exceptionJumpIndex) {
    super(opCode, exceptionJumpIndex);

    if (!OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException(opCode+" expects an argument.");
    }
  }

  @Override
  public String repr() {
    return opCode.name().toLowerCase();
  }
  
}
