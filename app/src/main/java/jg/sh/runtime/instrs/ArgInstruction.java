package jg.sh.runtime.instrs;

import jg.sh.compile.instrs.OpCode;

public class ArgInstruction extends RuntimeInstruction {

  private final int argument;

  public ArgInstruction(OpCode opCode, int argument, int exceptionJumpIndex) {
    super(opCode, exceptionJumpIndex);
    this.argument = argument;

    if (OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException(opCode+" expects no argument.");
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof ArgInstruction) {
      final ArgInstruction other = ((ArgInstruction) obj);
      return other.opCode == opCode && 
             other.exceptionJumpIndex == exceptionJumpIndex && 
             other.argument == argument;
    }
    return false;
  }
  
  public int getArgument() {
    return argument;
  }

  public String repr() {
    return opCode.name().toLowerCase()+" "+argument;
  }
}
