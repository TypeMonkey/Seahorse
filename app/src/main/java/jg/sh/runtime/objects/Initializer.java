package jg.sh.runtime.objects;

import jg.sh.runtime.objects.RuntimeInstance.AttrModifier;

/**
 * Invoked by RuntimeInstances in their consturctor
 * to set initial attributes.
 * @author Jose
 */
@FunctionalInterface
public interface Initializer {

  /**
   * Adds the gives attribute to the RuntimeInstance
   * @param name - the attribute name
   * @param value - the initial attribute value
   * @param modifiers - modifiers describing how this attribute should be interacted with
   */
  public void init(String name, RuntimeInstance value, AttrModifier ... modifiers);
}