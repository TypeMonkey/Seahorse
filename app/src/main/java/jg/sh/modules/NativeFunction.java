package jg.sh.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Represents a top-level function of a NativeModule.
 * 
 * Methods with this annotation should be static and have
 * four paramters:
 * 1.) A Fiber (the current Fiber it's being called in)
 * 2.) Either RuntimeInstance or a  subtype (the object this function is bound to)
 * 3.) A RuntimeInternalCallable (the Seahorse runtime representation of this function)
 * 4.) An ArgVector (holds function arguments)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NativeFunction {
  /**
   * The preferred name for this attribute, or the empty
   * string if the name of the method is to be used as the attribute name.
   * @return preferred name for this attribute, or the empty
   * string if the name of the method is to be used as the attribute name.
   */
  String name() default "";

  /**
   * The amount of positional arguments this function expects.
   * @return the amount of positional arguments this function expects.
   * 
   * The default is 0
   */
  int positionalParams() default 0;

  /**
   * The keyword/optional arguments this function can take in.
   * @return the keyword/optional arguments this function can take in.
   * 
   * The default is an empty String []
   */
  String[] optionalParams() default {};

  /**
   * If this function can take in a variable amount of arguments
   * after all positional arguments.
   * @return whether this function can take in a variable amount of arguments
   * after all positional arguments.
   * 
   * The default is false.
   */
  boolean hasVariableParams() default false;

  /**
   * If this function can take in a variable amount of keyword argumentss.
   * @return whether this function can take in a variable amount of keyword arguments.
   * 
   * The default is false.
   */
  boolean hasVarKeywordParams() default false;
}
