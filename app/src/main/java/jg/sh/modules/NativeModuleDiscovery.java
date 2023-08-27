package jg.sh.modules;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for a static, no-argument method, on a NativeModule class, that returns a RuntimeModule.
 * 
 * A NativeModule class should have one - and only one - such method. 
 * 
 * Classes that are exend NativeModule will be checked
 * for such condition. If such condition fails to hold, an exception will be thrown
 * at loading.
 * 
 * It's advised that the returned RuntimeModule should be a
 * singleton instance (i.e: the method shouldn't return a new instance each call)
 * 
 * @author Jose
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface NativeModuleDiscovery {

}
