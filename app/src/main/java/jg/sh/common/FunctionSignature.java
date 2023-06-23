package jg.sh.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jg.sh.compile.parsing.nodes.ReservedWords;

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
  public static final FunctionSignature NO_ARG = new FunctionSignature(0, Collections.emptySet());
  
  /**
   * A signature for a function that strictly accepts one positional argument.
   */
  public static final FunctionSignature ONE_ARG = new FunctionSignature(1, Collections.emptySet());
  
  private final int positionalParamCount;
  private final Set<String> keywordParams;
  
  /**
   * Constructs a FunctionSignature
   */
  public FunctionSignature(int positionalParamCount, Set<String> keywordParams) {
    this.positionalParamCount = positionalParamCount;
    this.keywordParams = keywordParams;
  }
  
  public FunctionSignature(int positionalParamCount) {
    this(positionalParamCount, Collections.emptySet());
  }
  
  public int getPositionalParamCount() {
    return positionalParamCount;
  }
  
  public Set<String> getKeywordParams() {
    return keywordParams;
  }
  
  /**
   * Creates a deep copy of this FunctionSignature with the given function name 
   * @param name - the name to use in the new FunctionSignature
   * @return a deep copy of this FunctionSignature with the given function name 
   */
  /*
  public FunctionSignature namedAs(String name) {
    return new FunctionSignature(name, modifiers, positionalParamCount, keywordParams, hasVariableParams);
  }
  */
  
  @Override
  public String toString() {    
    return "POS "+positionalParamCount+" , KEYS "+keywordParams;
  }
}
