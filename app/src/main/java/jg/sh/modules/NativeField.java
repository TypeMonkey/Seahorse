package jg.sh.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NativeField {
  
  /**
   * The name of this field in Seahorse. 
   * 
   * By default, this value is just the name of the annotated field.
   * However, be cautioned: object attributes and method must all have 
   * unique identifiers. If a NativeDataDefinition has an annotated field and
   * method that have the same name, one can accidentally override the other.
   * 
   * @return name of this field in Seahorse.
   */
  String name() default "";

}
