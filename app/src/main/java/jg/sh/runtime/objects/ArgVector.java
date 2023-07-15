package jg.sh.runtime.objects;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.exceptions.OperationException;

/**
 * A vector of arguments for a function call.
 */
public class ArgVector extends RuntimeInstance {
  
  private List<RuntimeInstance> positionals;
  
  public ArgVector() {
    this.positionals = new ArrayList<>();
  }
  
  public ArgVector(RuntimeInstance ... initialPositionals) {
    this.positionals = new ArrayList<>(Arrays.asList(initialPositionals));
  }

  /**
   * Does nothing.
   */
  @Override
  public void seal(){}

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
}
