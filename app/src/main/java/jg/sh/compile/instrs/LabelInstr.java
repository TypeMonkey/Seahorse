package jg.sh.compile.instrs;

import jg.sh.common.Location;

public class LabelInstr extends Instruction {
  
  private final String name;

  public LabelInstr(Location start, Location end, String name) {
    super(start, end, OpCode.LABEL);
    this.name = name;
  }
  
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return "~ "+name+":";
  }

}
