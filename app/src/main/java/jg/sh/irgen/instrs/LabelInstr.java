package jg.sh.irgen.instrs;

public class LabelInstr extends Instruction {
  
  private final String name;

  public LabelInstr(int line, int col, String name) {
    super(line, col, OpCode.LABEL);
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
