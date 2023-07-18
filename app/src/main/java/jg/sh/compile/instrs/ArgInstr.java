package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class ArgInstr extends Instruction{

  private final int argument;
  
  public ArgInstr(Location start, Location end, OpCode opCode, int argument) {
    super(start, end, opCode);
    if (OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException("'"+opCode.name().toLowerCase()+"' requires an argument");
    }
    
    this.argument = argument;
  }
  
  public int getArgument() {
    return argument;
  }

  @Override
  public String toString() {
    return opCode.name().toLowerCase()+" "+argument;
  }

}
