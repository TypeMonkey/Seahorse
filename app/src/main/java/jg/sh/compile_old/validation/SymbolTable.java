package jg.sh.compile_old.validation;

import java.util.HashMap;
import java.util.Map;

import jg.sh.common.presenters.VariablePresenter;
import jg.sh.compile_old.validation.Context.ContextType;

public class SymbolTable {
  
  private final SymbolTable parentTable;
  
  private final Map<String, SymbolContext> accessibleSymbols;
  private final Context tableContext;
  
  /**
   * Couples symbol information with the symbol's context
   * @author Jose
   */
  public static class SymbolContext {
    private final VariablePresenter presenter;
    private final Context context;
    
    public SymbolContext(VariablePresenter presenter, Context context) {
      this.presenter = presenter;
      this.context = context;
    }
    
    public VariablePresenter getPresenter() {
      return presenter;
    }
    
    public Context getContext() {
      return context;
    }

    @Override
    public String toString() {
      return "symbol '"+presenter.getName()+"' , context: "+context;
    }
  }
  
  /**
   * Constructs a SymbolTable
   * @param accessibleModules - a map of modules already accessible prior
   *                            to the creation of this SymbolTable
   *                            (like the standard modules!)
   * @param accesibleFunctions - a set of functions already accessible prior
   *                             to the creation of this SymbolTable
   *                             (like build-in functions!)
   *                             
   * NOTE: All collections are deep copied.
   */
  public SymbolTable(Context tableContext) {
    this(null, tableContext);
  } 
  
  public SymbolTable(SymbolTable parentTable, Context tableContext) {
    this.parentTable = parentTable;
    this.tableContext = tableContext;
    this.accessibleSymbols = new HashMap<>();
  }
  
  //DEV_NOTE: All "addXXXX" methods should return TRUE if no a similar object hasn't been added.
  //          FALSE if else
  
  public boolean addVariable(VariablePresenter presenter) {
    return accessibleSymbols.put(presenter.getName(), new SymbolContext(presenter, tableContext)) == null;
  }
  
  public boolean isSymbolAccessible(String symbolName) {
    return getSymbol(symbolName) != null;
  }
  
  public SymbolContext getSymbol(String symbolName) {
    return accessibleSymbols.containsKey(symbolName) ? 
        accessibleSymbols.get(symbolName) : (parentTable != null ? parentTable.getSymbol(symbolName) : null);
  }
  
  /**
   * Retrieves the table (including this table) that has the given ContextType
   * @param contextType - the ContextType to look for
   * @return the nearest (most immediate ancestor) SymbolTable that has the given ContextType , 
   *         or null if no such table with the corresponding context is found.
   */
  public SymbolTable getTable(ContextType contextType) {
    if (tableContext.getCurrentContextType() == contextType) {
      return this;
    }
    else if (parentTable != null) {
      return parentTable.getTable(contextType);
    }
    else {
      return null;
    }
  }
  
  public Context getTableContext() {
    return tableContext;
  }
  
  public Map<String, SymbolContext> getAccessibleSymbols() {
    return accessibleSymbols;
  }
  
  public SymbolTable getParentTable() {
    return parentTable;
  }
}
