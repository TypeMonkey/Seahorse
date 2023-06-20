package jg.sh.interpret.components;

import jg.sh.interpret.Interpreter;

/**
 * Represents an invokable sequence of instructions
 * that has its own scope and parameters
 * @author Jose
 */
public interface Callable {
  
  public int invoke(Interpreter interpreter);
 
  public boolean isCallable();
  
}