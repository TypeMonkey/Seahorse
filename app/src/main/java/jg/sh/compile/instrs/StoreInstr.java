package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class StoreInstr extends ArgInstr {
    
  public StoreInstr(Location start, Location end, OpCode initialOpCode, MutableIndex index) {
    super(start, end, initialOpCode, index);
  }
}
