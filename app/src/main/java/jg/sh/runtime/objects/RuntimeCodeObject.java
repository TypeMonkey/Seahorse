package jg.sh.runtime.objects;

import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.loading.ContextualInstr;

public class RuntimeCodeObject extends RuntimeInstance {

  private final String boundName;
  private final FunctionSignature signature;
  private final Map<String, Integer> keywordIndexes;
  private final ContextualInstr [] instrs;
  private final int [] captures;

  public RuntimeCodeObject(String boundName, FunctionSignature signature, Map<String, Integer> keywordIndexes, ContextualInstr [] instrs, int [] captures) {
    this.boundName = boundName;
    this.signature = signature;
    this.keywordIndexes = keywordIndexes;
    this.instrs = instrs;
    this.captures = captures;
  }
  
  public ContextualInstr [] getInstrs() {
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

}
