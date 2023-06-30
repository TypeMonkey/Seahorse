package jg.sh.compile.instrs;

import jg.sh.common.Location;

/**
 * Represents a jump instruction
 * @author Jose
 *
 */
public class JumpInstr extends Instruction{
  
  private final String label;

  public JumpInstr(Location start, Location end, OpCode opCode, String label) {
    super(start, end, opCode);
    if (!OpCode.isJumpInstr(opCode)) {
      throw new IllegalArgumentException("'"+opCode.name().toLowerCase()+"' isn't a jump op-code!");
    }
    
    this.label = label;
  }
  
  public String getTargetLabel() {
    return label;
  }

  @Override
  public String toString() {
    return opCode.name().toLowerCase()+" "+label;
  }
}
