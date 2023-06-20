package jg.sh.irgen.instrs;

public abstract class Instruction {
  
  private final int line;
  private final int col;
  
  protected OpCode opCode;
  
  public Instruction(int line, int col, OpCode opCode) {
    this.line = line;
    this.col = col;
    this.opCode = opCode;
  }
  
  public int getLine() {
    return line;
  }
  
  public int getCol() {
    return col;
  }
  
  public OpCode getOpCode() {
    return opCode;
  }
  
  public abstract String toString();
}
