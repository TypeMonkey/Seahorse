package jg.sh.runtime.threading.stackops;

import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

@FunctionalInterface
public interface TopModifier {
  
  /**
   * Processes the given RuntimeInstance, returning the result.
   * @param instance - the RuntimeInstance to process
   * @return the result of processing the RuntimeInstance, or null if the RuntimeInstance
   *         couldn't be processed.
   * @throws OperationException - thrown if the actual process done by this TopModifier throws it.
   */
  public RuntimeInstance modify(RuntimeInstance instance) throws OperationException ;

}
