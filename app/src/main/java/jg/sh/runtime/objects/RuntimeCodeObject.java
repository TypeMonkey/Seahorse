package jg.sh.runtime.objects;

import java.util.Arrays;
import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.instrs.RuntimeInstruction;

public class RuntimeCodeObject extends RuntimeInstance {

  private final String boundName;
  private final FunctionSignature signature;
  private final Map<String, Integer> keywordIndexes;
  private final RuntimeInstruction [] instrs;
  private final int [] captures;
  private final int varArgIndex;
  private final int keywordVarArgIndex;

  public RuntimeCodeObject(String boundName, 
                           FunctionSignature signature, 
                           Map<String, Integer> keywordIndexes, 
                           int varArgIndex,
                           int keywordVarArgIndex,
                           RuntimeInstruction [] instrs, 
                           int [] captures) {
    this.boundName = boundName;
    this.signature = signature;
    this.keywordIndexes = keywordIndexes;
    this.instrs = instrs;
    this.captures = captures;
    this.varArgIndex = varArgIndex;
    this.keywordVarArgIndex = keywordVarArgIndex;
  }
  
  public RuntimeInstruction [] getInstrs() {
    return instrs;
  }
  
  public String getBoundName() {
    return boundName;
  }
  
  public int [] getCaptures() {
    return captures;
  }
  
  public FunctionSignature getSignature() {
    return signature;
  }
  
  public Map<String, Integer> getKeywordIndexes() {
    return keywordIndexes;
  }

  public int getKeywordVarArgIndex() {
    return keywordVarArgIndex;
  }

  public int getVarArgIndex() {
    return varArgIndex;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof RuntimeCodeObject)) {
      return false;
    }
    RuntimeCodeObject other = (RuntimeCodeObject) obj;

    if(!boundName.equals(other.boundName)) {
      return false;
    }

    if (!signature.equals(other.signature)) {
      return false;
    }

    if (!keywordIndexes.equals(other.keywordIndexes)) {
      return false;
    }

    if (!Arrays.equals(instrs, other.instrs)) {
      return false;
    }

    if (!Arrays.equals(captures, other.captures)) {
      return false;
    }

    if (varArgIndex != other.varArgIndex) {
      return false;
    }
    if (keywordVarArgIndex != other.keywordVarArgIndex) {
      return false;
    }
    
    return true;
  }
}
