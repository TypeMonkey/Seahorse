package jg.sh.modules;

import java.util.Collections;

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
  
  protected NativeModule(String name) {
    this.runtimeModule = new RuntimeModule(name, null, Collections.emptyMap());
    
    this.loadingFunction = new InternalFunction(new FunctionSignature(Collections.emptySet(), 0, Collections.emptySet(), false)) {      
      @Override
      public RuntimeInstance invoke(Fiber executor, ArgVector args)
          throws InvocationException {
        initModule((RuntimeObject) args.getPositional(SELF_INDEX));
        return args.getPositional(SELF_INDEX);
      }
    };
  }
  
  public abstract void initModule(RuntimeObject object);
  
  public InternalFunction getLoadingFunction() {
    return loadingFunction;
  }
  
  public RuntimeModule getModule() {
    return runtimeModule;
  }
}
