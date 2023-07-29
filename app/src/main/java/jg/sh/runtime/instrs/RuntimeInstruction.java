package jg.sh.runtime.instrs;

import jg.sh.common.Location;
import jg.sh.compile.instrs.OpCode;

public abstract class RuntimeInstruction {
  
  protected final OpCode opCode;
  protected final int exceptionJumpIndex;

  protected Location start;
  protected Location end;

  public RuntimeInstruction(OpCode opCode, int exceptionJumpIndex) {
    this.opCode = opCode;
    this.exceptionJumpIndex = exceptionJumpIndex;
  }

  public RuntimeInstruction setStart(Location start) {
    this.start = start;
    return this;
  }

  public RuntimeInstruction setEnd(Location end) {
    this.end = end;
    return this;
  }

  public OpCode getOpCode() {
    return opCode;
  }

  public int getExceptionJumpIndex() {
    return exceptionJumpIndex;
  }

  public Location getEnd() {
    return end;
  }

  public Location getStart() {
    return start;
  }

  public boolean hasLocations() {
    return start != null && end != null;
  }

  public abstract String repr();

  public String toString() {
    return repr()+" => "+exceptionJumpIndex+(hasLocations() ? "   At "+start+" <-> "+end : "");
  }
}
