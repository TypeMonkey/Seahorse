package jg.sh.runtime.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jg.sh.runtime.exceptions.OperationException;

/**
 * A vector of arguments for a function call.
 */
public class ArgVector extends RuntimeInstance {

  /**
   * A singleton ArgVector instance meant to hold
   * no arguments (both positional and keyword).
   * 
   * This singleton is useful for function calls that have no arguments - 
   * as in once the argVector is pushed, the next instruction pops it 
   * to give to the callee.
   * 
   * Since ArgVector.seal() is overriden and does nothing,
   * this singleton isn't a sealed object, so by mistake, 
   * it CAN BE mutated - but it shouldn't.
   */
  public static final ArgVector EMPTY = new ArgVector();
  
  private List<RuntimeInstance> positionals;
  
  public ArgVector(RuntimeInstance ... initialPositionals) {
    this.positionals = new ArrayList<>(Arrays.asList(initialPositionals));
  }

  /**
   * Does nothing.
   */
  @Override
  public final void seal(){}

  public void setKeywordArg(String keyword, RuntimeInstance value) {
    try {
      setAttribute(keyword, value);
    } catch (OperationException e) {
      /*
       * Should never happen as ArgVectors are unsealable.
       * If it does, the world is doomed. Panic!
       */
      throw new Error(e);
    }
  }

  public void addAtFront(RuntimeInstance instance) {
    positionals.add(0, instance);
  }
  
  public void addPositional(RuntimeInstance instance) {
    positionals.add(instance);
  }
  
  public RuntimeInstance getPositional(int index) {
    return positionals.get(index);
  }
  
  public List<RuntimeInstance> getPositionals() {
    return positionals;
  }

  public String toString() {
    return "ARG_VERCTOR: "+positionals.size();
  }
}
