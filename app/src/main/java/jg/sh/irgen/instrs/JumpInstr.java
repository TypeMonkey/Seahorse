package jg.sh.irgen.instrs;

/**
 * Represents a jump instruction
 * @author Jose
 *
 */
public class JumpInstr extends Instruction{
  
  private final String label;

  public JumpInstr(int line, int col, OpCode opCode, String label) {
    super(line, col, opCode);
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
