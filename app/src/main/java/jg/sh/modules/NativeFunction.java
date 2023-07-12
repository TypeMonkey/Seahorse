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
  
}
