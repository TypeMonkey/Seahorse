package jg.sh.compile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.LoadStorePair;
import jg.sh.compile.instrs.StoreCellInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.parsing.Context;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.token.TokenType;

public class CompContext extends Context<CompContext> {

  public static enum ContextKey{
    /**
     * Value is a String
     */
    BREAK_LOOP_LABEL,
    
    /**
     * Value is a String
     */
    CONT_LOOP_LABEL,
    
    /**
     * Value is an int/Integer
     */
    LOCAL_VAR_INDEX,

    /**
     * Value is a VarAllocator
     */
    VAR_ALLOCATOR,
    
    /**
     * Value is a LoadCellInstruction
     * 
     * Code should fulfill the loading of the "self" object, relevant to the
     * current CompContext
     */
    SELF_CODE,

    /**
     * Value is a boolean
     * 
     * If this is referring to an L-Val and 
     * the instruction needs to be a STORE-like one.
     */
    NEED_STORAGE,
    
    /**
     * Value is an int/Integer
     */
    CL_VAR_INDEX;
  }
  
  public static enum ContextType {
    /*
     * Currently inside a module
     */
    MODULE, 
    
    /**
     * Currently inside an object literal
     */
    OBJECT,
    
    /*
     * Currently inside a function or method
     */
    FUNCTION,
    
    /*
     * Current inside a for-loop or while loop
     */
    LOOP,
    
    /*
     * Currently inside a scoped block
     */
    BLOCK;
  }

  /**
   * Houses information of an identifier - it's index and if it's a local variable or not.
   */
  public static class IdentifierInfo {
    private final CompContext context;
    
    private final Set<Keyword> descriptors;
    private final LoadStorePair pair;

    private IdentifierInfo(CompContext context, 
                           LoadStorePair pair,
                           Set<Keyword> descriptors) {
      this.context = context;
      this.pair = pair;
      this.descriptors = descriptors;
    }
    
    private IdentifierInfo(CompContext context, 
                           LoadStorePair pair,
                           Keyword ... descriptors) {
      this(context, pair, new HashSet<>(Arrays.asList(descriptors)));
    }
    
    public LoadCellInstr getLoadInstr() {
      return pair.load;
    }
    
    public StoreCellInstr getStoreInstr() {
      return pair.store;
    }

    public LoadStorePair getPairInstr() {
      return pair;
    }
    
    public CompContext getContext() {
      return context;
    }

    public Set<Keyword> getDescriptors() {
      return descriptors;
    }

    public boolean isConstant() {
      return Keyword.hasKeyword(TokenType.CONST, descriptors);
    }

    public boolean isExported() {
      return Keyword.hasKeyword(TokenType.EXPORT, descriptors);
    }
    
    @Override
    public String toString() {
      return "@var || "+context+" || LOAD: "+pair.load+" || STORE: "+pair.store;
    }
  }
  
  private final Map<ContextKey, Object> contextMap;
  private final Map<String, IdentifierInfo> varMap;
  private final ContextType currentContext;
  private final ConstantPool constantPool;

  public CompContext(ContextType currentContext, ConstantPool constantPool) {
    this(null, currentContext, constantPool);
  }

  public CompContext(CompContext parent, ContextType currentContext) {
    super(parent);
    this.contextMap = new HashMap<>();
    this.varMap = new HashMap<>();
    this.currentContext = currentContext;
    this.constantPool = parent.constantPool;
  } 
  
  public CompContext(CompContext parent, ContextType currentContext, ConstantPool constantPool) {
    super(parent);
    this.contextMap = new HashMap<>();
    this.varMap = new HashMap<>();
    this.currentContext = currentContext;
    this.constantPool = constantPool;
  } 

  /**
   * Adds a variable to this context manager's immediate variable map
   * @param varName - the varName 
   * @param pair - the LoadStorePair for loading/storing the given variable
   * @param keywords - Keyword descriptors regarding the variable
   * @return false - if a variable with the same name is already in this ContextManager, true if else
   */
  public boolean addVariable(String varName, LoadStorePair pair, Keyword ... descriptors) {
    if (varMap.containsKey(varName)) {
      return false;
    }

    varMap.put(varName, new IdentifierInfo(this, pair, descriptors));
    return true;
  }

  /**
   * Adds a variable to this context manager's immediate variable map
   * @param varName - the varName 
   * @param pair - the LoadStorePair for loading/storing the given variable
   * @param keywords - Keyword descriptors regarding the variable
   * @return false - if a variable with the same name is already in this ContextManager, true if else
   */
  public boolean addVariable(String varName, LoadStorePair pair, Set<Keyword> descriptors) {
    if (varMap.containsKey(varName)) {
      System.out.println(" --- context contains "+varName);
      return false;
    }

    varMap.put(varName, new IdentifierInfo(this, pair, descriptors));
    return true;
  }

  public IdentifierInfo getDirect(String varName) {
    return varMap.get(varName);
  }

  public IdentifierInfo getVariable(String varName) {
    final CompContext compContext = search(c -> c.varMap.containsKey(varName), true);
    return compContext != null ? compContext.getDirect(varName) : null;
  }
  
  public void setContextValue(ContextKey key, Object value) {
    contextMap.put(key, value);
  }

  public boolean hasContextValue(ContextKey contextKey) {
    return search(c -> c.contextMap.containsKey(contextKey), true) != null;
  }

  public Object getValue(ContextKey contextKey) {
    final CompContext compContext = search(c -> c.contextMap.containsKey(contextKey), true);
    return compContext != null ? compContext.contextMap.get(contextKey) : null;
  }
  
  public Map<ContextKey, Object> getContextMaps() {
    return contextMap;
  }
  
  public ContextType getCurrentContext() {
    return currentContext;
  }
  
  public boolean isWithinContext(ContextType contextType) {
    return getNearestContext(contextType) != null;
  }

  public CompContext getNearestContext(ContextType contextType) {
    return search(c -> c.currentContext == contextType, true);
  }
  
  public ConstantPool getConstantPool() {
    return constantPool;
  }
}
