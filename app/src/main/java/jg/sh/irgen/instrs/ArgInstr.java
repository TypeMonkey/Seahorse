package jg.sh.irgen.instrs;

public class ArgInstr extends Instruction{

  private final int argument;
  
  public ArgInstr(int line, int col, OpCode opCode, int argument) {
    super(line, col, opCode);
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
