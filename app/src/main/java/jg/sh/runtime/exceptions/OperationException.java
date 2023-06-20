package jg.sh.runtime.exceptions;

/**
 * An exception to indicate that a certain object cannot perform 
 * a non-arithmetic operator - either because it doesn't support it, or an error
 * occured while performing it.
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
