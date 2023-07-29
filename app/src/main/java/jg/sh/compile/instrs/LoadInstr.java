package jg.sh.compile.instrs;

import jg.sh.common.Location;

/**
 * Specifies a LOAD_XXX instruction.
 * 
 * @author Jose
 */
public class LoadInstr extends ArgInstr {
  
  public LoadInstr(Location start, Location end, OpCode initialOpCode, MutableIndex index) {
    super(start, end, initialOpCode, index);
  }

}
