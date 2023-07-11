package jg.sh.modules;

import java.util.Collections;
import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeObject;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.threading.fiber.Fiber;

/**
 * Represents a class that produces a native module (a module with Java-based components)
 * 
 * Module Implementation:
 * Native modules, and all their immediate components, should be housed in a single class (which should
 * in turn have this annotation applied)
 * 
 * Classes with the NativeModule annotation must have one - and only one - static method
 * that return the RuntimeModule representing that module.
 * 
 * This static method must be marked with the NativeModuleDiscovery annotation.
 * 
 * 
 * 
 * Module Loading:
 * At module loading, .class file will be loaded and checked if they are annotated with the NativeModule annotation.
 * Only classes with such annotation are considered, and such classes
 * will be checked on whether they have one - and only one - static method that returns a RuntimeModule.
 * 
 * If this check fails, an exception at loading is thrown. Else, the RuntimeModule returned by the 
 * method is loaded.
 * 
 * @author Jose
 */

public abstract class NativeModule {

  protected final RuntimeModule runtimeModule;
  private final InternalFunction loadingFunction;
  
  protected NativeModule() {
    this.runtimeModule = new RuntimeModule(getName(), null, Collections.emptyMap());
    
    this.loadingFunction = new InternalFunction(FunctionSignature.NO_ARG) {      
      @Override
      public RuntimeInstance invoke(Fiber executor, ArgVector args)
          throws InvocationException {
        initialize((RuntimeObject) args.getPositional(SELF_INDEX));
        return args.getPositional(SELF_INDEX);
      }
    };
  }
  
  /**
   * This method should focus on initializing the RuntimeObject of a Module
   * with initial attributes. Any other intialization tasks outside that
   * should be put in initialize();
   * 
   * The method is invoked directly when the moduleObject is allocated, meaning
   * if there are lengthy tasks in this method, it has the potential to substantially
   * hold up execution.
   * 
   * @param moduleObject - the RuntimeObject for this NativeModule
   * @param map - the attribute map of the RuntimeObject
   */
  public abstract void initialAttrs(RuntimeObject moduleObject, Map<String, RuntimeInstance> map);

  /**
   * Performs initialization tasks for this NativeModule when loaded. Heavier initialization tasks - such as 
   * loading of external resources, etc. - should be put in here.
   * 
   * The Seahorse interpreter will schedule the execution of this method like any other
   * frame in the Fiber this NativeModule was loaded into, allowing for flexibility.
   * 
   * @param moduleObject - the RuntimeObject for this NativeModule
   */
  public abstract void initialize(RuntimeObject moduleObject);

  public abstract String getName();
  
  public InternalFunction getLoadingFunction() {
    return loadingFunction;
  }
  
  public RuntimeModule getModule() {
    return runtimeModule;
  }
}
