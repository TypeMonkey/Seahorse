package jg.sh.compile.instrs;

import jg.sh.common.Location;

/**
 * Specifies a LOAD_XXX instruction.
 * 
 * The reason it's named LoadCellInstr and not just LoadInstr is because of one thing:
 * capture variables.
 * 
 * Some compilers will do two passes over a function to analyze which variable were captured
 * by a closure and make specific adjustments to the loading and storing of those variables.
 * 
 * With LoadCellInstr, we can effectively do a single pass and when we encounter a closure,
 * we can change the store/load instructions of variables we previously thought were not captured
 * 
 * @author Jose
 *
 */
public class LoadCellInstr extends Instruction{

  private int index;
  
  public LoadCellInstr(Location start, Location end, OpCode initialOpCode, int index) {
    super(start, end, initialOpCode);
    this.index = index;
  }

  public void setIndex(int index) {
    this.index = index;
  }
  
  public void setOpCode(OpCode opCode) {
    this.opCode = opCode;
  }
  
  public int getIndex() {
    return index;
  }
  
  @Override
  public String toString() {
    return opCode.name().toLowerCase()+" "+index;
  }

}
