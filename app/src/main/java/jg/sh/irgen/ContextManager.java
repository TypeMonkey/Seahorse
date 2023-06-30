package jg.sh.irgen;

import java.util.HashMap;
import java.util.Map;

import jg.sh.irgen.instrs.LoadCellInstr;
import jg.sh.irgen.instrs.StoreCellInstr;

public class ContextManager {
  
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
     * Value is a Instruction[]
     * 
     * Code should fulfill the loaidng of the "self" object
     */
    SELF_CODE,
    
    /**
     * Value is an int/Integer
     */
    CL_VAR_INDEX;
  }
  
  public enum ContextType {
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
    private final ContextManager context;
    
    private final LoadCellInstr loadInstr;
    private final StoreCellInstr storeInstr;
    
    private IdentifierInfo(ContextManager context, LoadCellInstr loadInstr, StoreCellInstr storeInstr) {
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
    
    public ContextManager getContext() {
      return context;
    }
    
    @Override
    public String toString() {
      return "@var || "+context+" || LOAD: "+loadInstr+" || STORE: "+storeInstr;
    }
  }
  
  private final ContextManager parent;
  private final Map<ContextKey, Object> contextMap;
  private final Map<String, IdentifierInfo> varMap;
  private final ContextType currentContext;
  
  public ContextManager(ContextType currentContext) {
    this(null, currentContext);
  }
  
  public ContextManager(ContextManager parent, ContextType currentContext) {
    this.parent = parent;
    this.contextMap = new HashMap<>();
    this.varMap = new HashMap<>();
    this.currentContext = currentContext;
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
    return varMap.containsKey(varName) ? 
        varMap.get(varName) : 
          (parent != null ? parent.getVariable(varName) : null);
  }
  
  public void setContextValue(ContextKey key, Object value) {
    contextMap.put(key, value);
  }

  public boolean hasContextValue(ContextKey contextKey) {
    return contextMap.containsKey(contextKey) ? 
        true : 
          (parent != null ? parent.hasContextValue(contextKey) : false);
  }

  public Object getValue(ContextKey contextKey) {
    return contextMap.containsKey(contextKey) ? 
        contextMap.get(contextKey) : 
          (parent != null ? parent.getValue(contextKey) : null);
  }
  
  public Map<ContextKey, Object> getContextMaps() {
    return contextMap;
  }
  
  public ContextType getCurrentContext() {
    return currentContext;
  }
  
  public boolean isWithinContext(ContextType contextType) {
    return currentContext == contextType ? true : 
      (parent != null ? parent.isWithinContext(contextType) : false);
  }
}
