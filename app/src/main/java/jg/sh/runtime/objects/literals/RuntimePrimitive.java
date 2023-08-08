package jg.sh.runtime.objects.literals;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.objects.Initializer;
import jg.sh.runtime.objects.RuntimeInstance;

/**
 * Represents a runtime literal - numerical values, strings, booleans, and floats.
 * 
 * Yes, literal here is used loosely. In essence, these values are called literals 
 * because they are LITTERAL themselves. An integer is just an integer, a string a string, etc.
 * Unlike the other objects, these values are not a union of others.
 * 
 * @author Jose
 *
 */
public abstract class RuntimePrimitive extends RuntimeInstance {

  public RuntimePrimitive() {}
  
  @Override
  public void gcMark(Cleaner cleaner) {
    return;
  }

  public abstract boolean equals(Object obj); 

  public abstract String toString();
}
