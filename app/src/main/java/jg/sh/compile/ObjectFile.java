package jg.sh.compile;

import java.util.List;

import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool;

/**
 * A compiled Seahorse module containing its 
 * contant pool and instructions (Seahorse VM bytecode)
 */
public class ObjectFile {

  private final String name;
  private final String moduleLabelStart;
  private final ConstantPool pool;
  private final List<Instruction> moduleInstrs;

  public ObjectFile(String name, String moduleLabelStart, ConstantPool pool, List<Instruction> moduleInstrs) {
    this.name = name;
    this.moduleLabelStart = moduleLabelStart;
    this.pool = pool;
    this.moduleInstrs = moduleInstrs;
  }
  
  public List<Instruction> getModuleInstrs() {
    return moduleInstrs;
  }
  
  public ConstantPool getPool() {
    return pool;
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
    for(int i = 0; i < pool.getPoolSize(); i++) {
      x += "  "+i+" : "+pool.getComponent(i)+System.lineSeparator();
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
