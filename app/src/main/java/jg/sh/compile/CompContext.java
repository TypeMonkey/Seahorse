package jg.sh.compile;

import java.util.HashMap;
import java.util.Map;

import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.StoreCellInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.parsing.Context;

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
  }

  /**
   * Houses information of an identifier - it's index and if it's a local variable or not.
   */
  public static class IdentifierInfo {
    private final CompContext context;
    
    private final LoadCellInstr loadInstr;
    private final StoreCellInstr storeInstr;
    
    private IdentifierInfo(CompContext context, LoadCellInstr loadInstr, StoreCellInstr storeInstr) {
      this.context = context;
      this.loadInstr = loadInstr;
      this.storeInstr = storeInstr;
    }
    
    public LoadCellInstr getLoadInstr() {
      return loadInstr;
    }
    
    public StoreCellInstr getStoreInstr() {
      return storeInstr;
    }
    
    public CompContext getContext() {
      return context;
    }
    
    @Override
    public String toString() {
      return "@var || "+context+" || LOAD: "+loadInstr+" || STORE: "+storeInstr;
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
   * @param loadInstr - the instruction to use to load this variable's value
   * @param storeInstr - the instruction to use to change this variable's value
   * @return false - if a variable with the same name is already in this ContextManager, true if else
   */
  public boolean addVariable(String varName, LoadCellInstr loadInstr, StoreCellInstr storeInstr) {
    if (varMap.containsKey(varName)) {
      return false;
    }

    varMap.put(varName, new IdentifierInfo(this, loadInstr, storeInstr));
    return true;
  }

  public IdentifierInfo getVariable(String varName) {
    return search(c -> c.varMap.containsKey(varName), true).getVariable(varName);
  }
  
  public void setContextValue(ContextKey key, Object value) {
    contextMap.put(key, value);
  }

  public boolean hasContextValue(ContextKey contextKey) {
    return search(c -> c.contextMap.containsKey(contextKey), true) != null;
  }

  public Object getValue(ContextKey contextKey) {
    return search(c -> c.contextMap.containsKey(contextKey), true).getValue(contextKey);
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
