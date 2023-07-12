package jg.sh.runtime.exceptions;

/**
 * An exception to indicate that a certain object cannot perform 
 * a non-arithmetic operator - either because it doesn't support it, or an error
 * occured while performing it.
 * 
 * A few examples of when an OperationException may be thrown:
 * - Indexing a RuntimeInstance that doesn't support it, like: 10[0]
 * - Mutating a sealed RuntimeInstance
 * - Mutating a constant attribute of a RuntimeObject
 * 
 * For example, an OperationException can be thrown in the following example:
 * 
 *    10[0]
 *    
 * An integer isn't indexable, so it cannot perform an index operation
 * 
 * @author Jose
 *
 */
public class OperationException extends Exception {

  public OperationException(String message) {
    super(message);
  }
  
}
