package jg.sh.compile.pool.component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.instrs.Instruction;

public class CodeObject implements PoolComponent{
  
  private final FunctionSignature signature;
  private final String boundName;
  private final Map<String, Integer> keywordIndexes;
  private final List<Instruction> instrs;
  private final int [] captures;
  //private final boolean [] constantCaptures;
  
  public CodeObject(FunctionSignature signature, String boundName, Map<String, Integer> keywordIndexes, List<Instruction> instrs, int [] captures) {
    this.signature = signature;
    this.boundName = boundName;
    this.keywordIndexes = keywordIndexes;
    this.instrs = instrs;
    this.captures = captures;
    
    /*
    this.constantCaptures = constantCaptures;
    
    if (captures.length != constantCaptures.length) {
      throw new IllegalArgumentException("captures array and constantCaptures array must be of equal size!");
    }
    */
  }
  
  @Override
  public boolean equals(Object obj) {
    return this == obj;
  }
  
  public String getBoundName() {
    return boundName;
  }
  
  public int[] getCaptures() {
    return captures;
  }
  
  public Map<String, Integer> getKeywordIndexes() {
    return keywordIndexes;
  }
  
  /*
  public boolean[] getConstantCaptures() {
    return constantCaptures;
  }
  */
  
  public List<Instruction> getInstrs() {
    return instrs;
  }
  
  public FunctionSignature getSignature() {
    return signature;
  }

  @Override
  public String toString() {
    return "<code obj> "+boundName+System.lineSeparator()+
           "     -> Captures "+Arrays.toString(captures)+System.lineSeparator()+
                         instrs.stream()
                               .map(x -> "        "+x.toString())
                               .collect(Collectors.joining(System.lineSeparator()));
  }

  @Override
  public ComponentType getType() {
    return ComponentType.CODE;
  }
}
