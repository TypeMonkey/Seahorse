package jg.sh.irgen.instrs;

/**
 * Describes instructions that do not have arguments.
 * 
 * Currently, such instructions are:
 *  - Arithmetic instructions (add, sub, div, mod, neg, etc...) 
 *  - Boolean and bitwise instructions (band, bnor, and, or, not)
 *  - the function return instruction (ret)
 *  - Attribute retrieval and storage (loadattr, storeattr)
 * @author Jose
 *
 */
public class NoArgInstr extends Instruction{

  public NoArgInstr(int line, int col, OpCode opCode) {
    super(line, col, opCode);
    if (!OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException("'"+opCode.name().toLowerCase()+"' isn't a no-arg opcode!");
    }
  }

  @Override
  public String toString() {
    return opCode.name().toLowerCase();
  }
}
