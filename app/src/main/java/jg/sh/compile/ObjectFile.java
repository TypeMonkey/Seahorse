package jg.sh.compile;

import java.util.List;

import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.PoolComponent;

/**
 * A compiled Seahorse module containing its 
 * contant pool and instructions (Seahorse VM bytecode)
 */
public class ObjectFile {

  private final String name;
  private final String moduleLabelStart;
  private final List<PoolComponent> constants;
  private final List<Instruction> moduleInstrs;

  public ObjectFile(String name, String moduleLabelStart, List<PoolComponent> constants, List<Instruction> moduleInstrs) {
    this.name = name;
    this.moduleLabelStart = moduleLabelStart;
    this.constants = constants;
    this.moduleInstrs = moduleInstrs;
  }
  
  public List<Instruction> getModuleInstrs() {
    return moduleInstrs;
  }
  
  public List<PoolComponent> getConstants() {
    return constants;
  }
  
  public String getModuleLabelStart() {
    return moduleLabelStart;
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public String toString() {
    String x = "Compiled module '"+name+"'"+System.lineSeparator();
    
    x += "-------CONSTANT POOL-------"+System.lineSeparator();
    for(int i = 0; i < constants.size(); i++) {
      x += "  "+i+" : "+constants.get(i)+System.lineSeparator();
    }
    x += "-------CONSTANT POOL-------"+System.lineSeparator();
    
    x += "CODE: "+System.lineSeparator();
    for (Instruction instruction : moduleInstrs) {
      if (instruction.getOpCode() == OpCode.LABEL) {
        x += "   "+instruction+System.lineSeparator();
      }
      else {
        x += "        "+instruction+System.lineSeparator();
      }
    }
    
    return x;
  }
}
