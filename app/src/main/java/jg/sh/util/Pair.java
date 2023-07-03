package jg.sh.util;

/**
 * A pair of objects (tuple).
 * 
 * Once a Pai is instantiated, it's contents
 * cannot be reassgined.
 */
public class Pair<T,V> {
  
  public final T first;
  public final V second;

  public Pair(T first, V second) {
    this.first = first;
    this.second = second;
  }

}
