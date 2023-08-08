package jg.sh.common;

import java.util.Collections;
import java.util.Set;

/**
 * Represents a function's signature, which is composed of its
 * declared name and parameter details
 * 
 * 
 * @author Jose
 */
public class FunctionSignature {
  
  /**
   * A signature for a function that strictly accepts no arguments.
   */
  public static final FunctionSignature NO_ARG = new FunctionSignature(0, Collections.emptySet(), false, false);
  
  /**
   * A signature for a function that strictly accepts one positional argument.
   */
  public static final FunctionSignature ONE_ARG = new FunctionSignature(1, Collections.emptySet(), false, false);
  
  private final int positionalParamCount;
  private final Set<String> keywordParams;
  private final boolean hasVariableParams;
  private final boolean hasVarKeywordParams;
  
  /**
   * Constructs a FunctionSignature
   */
  public FunctionSignature(int positionalParamCount, Set<String> keywordParams, boolean hasVariableParams, boolean hasVarKeywordParams) {
    this.positionalParamCount = positionalParamCount;
    this.keywordParams = keywordParams == null ? Collections.emptySet() : keywordParams;
    this.hasVariableParams = hasVariableParams;
    this.hasVarKeywordParams = hasVarKeywordParams;
  }
  
  public FunctionSignature(int positionalParamCount, Set<String> keywordParams) {
    this(positionalParamCount, keywordParams, false, false);
  }

  public boolean equals(Object obj) {
    if (obj instanceof FunctionSignature) {
      final FunctionSignature sig = (FunctionSignature) obj;
      return sig.positionalParamCount == positionalParamCount && 
             sig.hasVariableParams == hasVariableParams &&
             sig.hasVarKeywordParams == hasVarKeywordParams &&
             sig.keywordParams.containsAll(keywordParams) &&
             keywordParams.containsAll(sig.keywordParams);
    }
    return false;
  }

  public int getPositionalParamCount() {
    return positionalParamCount;
  }
  
  public Set<String> getKeywordParams() {
    return keywordParams;
  }

  public boolean hasVariableParams() {
    return hasVariableParams;
  }

  public boolean hasVarKeywordParams() {
    return hasVarKeywordParams;
  }
  
  @Override
  public String toString() {    
    return "POS "+positionalParamCount+" , KEYS "+keywordParams;
  }
}
