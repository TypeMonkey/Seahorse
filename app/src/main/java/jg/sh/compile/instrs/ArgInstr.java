package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class ArgInstr extends Instruction implements MutableInstr {

  private final MutableIndex argument;
  
  public ArgInstr(Location start, Location end, OpCode opCode, MutableIndex argument) {
    super(start, end, opCode);
    if (OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException("'"+opCode.name().toLowerCase()+"' requires an argument");
    }
    
    this.argument = argument;
  }

  public void setArgument(int newArg) {
    this.argument.setIndex(newArg);
  }
  
  public MutableIndex getArgument() {
    return argument;
  }

  @Override
  public String toString() {
    return opCode.name().toLowerCase()+" "+argument;
  }

  @Override
  public void setOpCode(OpCode op) {
    this.opCode = op;
  }
}
