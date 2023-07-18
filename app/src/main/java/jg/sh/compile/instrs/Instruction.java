package jg.sh.compile.instrs;

import jg.sh.common.Location;

public abstract class Instruction {
  
  private final Location start;
  private final Location end;
  
  protected OpCode opCode;
  
  public Instruction(Location start, Location end, OpCode opCode) {
    this.start = start;
    this.end = end;
    this.opCode = opCode;
  }
  
  public Location getStart() {
    return start;
  }
  
  public Location getEnd() {
    return end;
  }
  
  public OpCode getOpCode() {
    return opCode;
  }
  
  public abstract String toString();
}
