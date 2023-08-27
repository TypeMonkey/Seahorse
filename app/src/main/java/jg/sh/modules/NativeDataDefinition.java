package jg.sh.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface NativeDataDefinition {
  
  /**
   * The NativeModule this data definition residers in.
   * 
   * Note: If no class is provided 
   * @return the NativeModule this data definition residers in.
   */
  Class<? extends NativeModule> hostModule() default NativeModule.class;

  String name() default "";

  boolean isSealed();
}
