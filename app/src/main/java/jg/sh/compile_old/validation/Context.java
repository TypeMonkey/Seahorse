package jg.sh.compile_old.validation;

/**
 * Stores the contextual location (what component is this component in?) of a
 * node in the syntax tree.
 * 
 * Contextual information is hierarchical, so for example:
 * 
 *   class Sample {
 *     
 *     func someMethod(){
 *        return 10; 
 *     }
 *   }
 *   
 *  The most immediate context of the statement "return 10" is FUNCTION (it's inside a function)
 *  But, it's also within a class (Sample), which is then within a module
 *  
 *  So, if we call isWithinContext() woth MODULE, CLASS, FUNCTION, the method will return true for all.
 * 
 * @author Jose
 */
public class Context {
  
  public enum ContextType{
    /*
     * Currently inside a module
     */
    MODULE, 
    
    /*
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
    BLOCK
  }
  
  private final ContextType contextType;
  private final Context parentContext;
  
  /**
   * Constructs a new Context with no parent context and the 
   * context type being MODULE
   */
  public Context() {
    this(ContextType.MODULE, null);
  }
  
  public Context(ContextType contextType, Context parentContext) {
    this.contextType = contextType;
    this.parentContext = parentContext;
  }
  
  public boolean isWithinContext(ContextType type) {
    return type == contextType ? true : (parentContext != null ? parentContext.isWithinContext(type) : false);
  }
  
  public Context getClosestContext(ContextType type) {
    return type == contextType ? this : (parentContext != null ? parentContext.getClosestContext(type) : null);
  }
  
  public ContextType getCurrentContextType() {
    return contextType;
  }
  
  public Context getParentContext() {
    return parentContext;
  }
  
  @Override
  public String toString() {
    String xString = "CONTEXT: ";
    Context current = this;
    while(current != null) {
      xString += current.contextType+" -> ";
      current = current.parentContext;
    }
    return xString;
  }
}
