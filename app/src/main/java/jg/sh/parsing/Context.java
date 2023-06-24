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
public abstract class Context {

  private final Node node;
  private final Context parent;

  public Context(Node node) {
    this(node, null);
  }

  public Context(Node node, Context parent) {
    this.node = node;
    this.parent = parent;
  }

  /**
   * Given a predicate to check against, this method traverses the 
   * ancestral tree of this Context for a matching Context.
   * @param predicate - the function to use to check for a matching Context
   * @param includeSelf - whether to test the current Context 
   * @return the matching Context, or null if no such Context was found
   */
  public Context search(Predicate<Context> predicate, boolean includeSelf) {
    if (includeSelf && predicate.test(this)) {
      return this;
    }
    return parent != null ? parent.search(predicate, true) : null;
  }

  public Node node() {
    return node;
  }

  public Context parent() {
    return parent;
  }

  public boolean hasParent() {
    return parent != null;
  }
}
