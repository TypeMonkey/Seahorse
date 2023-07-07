package jg.sh.parsing;

import java.util.function.Predicate;

import jg.sh.parsing.nodes.Node;

/**
 * Provides contextual information when traversing
 * each node on the syntax tree.
 * 
 * A Context keeps track of both any contextual information, as
 * well as it's parent Context.
 */
public abstract class Context<T extends Context<?>> {

  private final T parent;

  public Context(T parent) {
    this.parent = parent;
  }

  /**
   * Given a predicate to check against, this method traverses the 
   * ancestral tree of this Context for a matching Context.
   * @param predicate - the function to use to check for a matching Context
   * @param includeSelf - whether to test the current Context 
   * @return the matching Context, or null if no such Context was found
   */
  public T search(Predicate<T> predicate, boolean includeSelf) {
    T current = includeSelf ? (T) this : parent;
    while (current != null) {
      if(predicate.test(current)) {
        return current;
      }
      else {
        current = (T) current.parent();
      }
    }

    return null;
  }

  public T parent() {
    return parent;
  }

  public boolean hasParent() {
    return parent != null;
  }
}
