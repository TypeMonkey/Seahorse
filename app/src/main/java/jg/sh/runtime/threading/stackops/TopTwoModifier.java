package jg.sh.runtime.threading.stackops;

import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.objects.RuntimeInstance;

public interface TopTwoModifier {
  
  /**
   * Given the first and second top elements of the operand stack,
   * this method processes them and returns a RuntimeInstance.
   * @param topInstance - the top-most RuntimeInstance
   * @param secondInstance - the second top-most RuntimeInstance
   * @return the result of processing the RuntimeInstance, or null if the modification
   *         cannot be successfully processed.
   * @throws OperationException - thrown if modifcation fails exceptionally
   */
  public RuntimeInstance modify(RuntimeInstance topInstance, RuntimeInstance secondInstance) throws OperationException;
}
