package jg.sh.modules.builtin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.NativeFunction;
import jg.sh.modules.NativeModule;
import jg.sh.modules.NativeModuleDiscovery;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.RuntimeObject;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimePrimitive;
import jg.sh.runtime.objects.literals.RuntimeString;
import jg.sh.runtime.threading.fiber.Fiber;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;

/**
 * Represents the "system" module.
 * 
 * This module houses a collection of important and useful functions and
 * values, ranging from functions dealing with I/O to arithmetic. 
 * @author Jose
 */
public class SystemModule extends NativeModule {
  public static final String SYSTEM_NAME = "system";
  
  private static SystemModule runtimeModule_INSTANCE = new SystemModule();
  
  private final BufferedReader INPUT_READER;
  
  private SystemModule() {        
    this.INPUT_READER = new BufferedReader(new InputStreamReader(System.in));
  }

  @Override
  public void initialize(RuntimeObject moduleObject) {}

  @Override
  public void initialAttrs(RuntimeObject systemObject, Map<String, RuntimeInstance> attrs) {}

  @NativeFunction(hasVariableParams = true, optionalParams = {}, positionalParams = 0)
  public RuntimeInstance println(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) {
    for(int i = ARG_INDEX; i < args.getPositionals().size(); i++) {
      System.out.print(args.getPositional(i));
    }
    
    System.out.println();
    return RuntimeNull.NULL;
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance print(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) {
    System.out.print(args.getPositional(ARG_INDEX));
    return RuntimeNull.NULL;
  }

  @NativeFunction(positionalParams = 2)
  public RuntimeInstance bind(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    RuntimeInstance targetObject = args.getPositional(ARG_INDEX);
    RuntimeInstance targetFunction = args.getPositional(ARG_INDEX + 1);
    //System.out.println("  **** "+targetObject.getClass()+" , "+targetFunction.getClass());
    
    if (targetFunction instanceof Callable) {
      RuntimeCallable targetCallable = (RuntimeCallable) targetFunction;
      return targetCallable.rebind(targetObject, fiber.getHeapAllocator());
    }
    
    throw new InvocationException("Object provided isn't a callable", (Callable) args.getPositional(0));
  }

  @NativeFunction(hasVariableParams = true)
  public RuntimeInstance input(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    /*
     * Basically does what println does first before getting input
     */
    for(int i = ARG_INDEX; i < args.getPositionals().size(); i++) {
      System.out.print(args.getPositional(i));
    }       
    System.out.println();
    
    try {
      return fiber.getHeapAllocator().allocateString(INPUT_READER.readLine());
    } catch (IOException e) {
      throw new InvocationException("IO Exception encountered: "+e.getMessage(), (Callable) args.getPositional(0));
    }
  }
  
  @NativeFunction(positionalParams = 1)
  public RuntimeInstance load(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    RuntimeInstance moduleName = args.getPositional(ARG_INDEX);
    if (moduleName instanceof RuntimeString) {
      String actualName = ((RuntimeString) moduleName).getValue();
      
      RuntimeModule module = null;
      try {
        module = fiber.getFinder().load(actualName);
      } catch (Exception e) {
        throw new InvocationException("Exception while loading module '"+actualName+"' : "+e.getMessage(), (Callable) args.getPositional(0));     
      }
      
      if (module == null) {
        throw new InvocationException("'"+actualName+"' cannot be found", (Callable) args.getPositional(0));     
      }
      else if(!module.isLoaded()){
        module.setAsLoaded(true);
        fiber.queue(module.getModuleCallable());
      }
      
      return module.getModuleObject();
    }
    throw new InvocationException("String module name expected!", (Callable) args.getPositional(0));
  }

  @NativeFunction
  public RuntimeInstance now(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber.getHeapAllocator().allocateInt(System.nanoTime());
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance isArray(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimeArray);
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance isFunction(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof Callable);
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance isPrimitive(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimePrimitive);
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance isError(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimeError);
  }

  @NativeFunction(positionalParams = 1)
  public static RuntimeInstance spinOff(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    RuntimeInstance argument = args.getPositional(ARG_INDEX); 
    if (argument instanceof Callable) {
      return fiber.getManager().spinFiber( (Callable) argument, new ArgVector());
    }
    else {
      throw new InvocationException("Callable expected", (Callable) args.getPositional(0));     
    }
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance makeThread(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    RuntimeInstance argument = args.getPositional(ARG_INDEX); 
    if (argument instanceof Callable) {
      return fiber.getManager().makeThread((Callable) argument);
    }
      
    throw new InvocationException("Callable expected", (Callable) args.getPositional(0));
  }

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance toString(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    RuntimeInstance argument = args.getPositional(ARG_INDEX); 
    return fiber.getHeapAllocator().allocateString(argument.toString());
  } 

  @NativeFunction(positionalParams = 1)
  public RuntimeInstance exit(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    long exitCode = args.getPositional(ARG_INDEX) instanceof RuntimeInteger ?
                         ((RuntimeInteger) args.getPositional(ARG_INDEX)).getValue() : 0;
    System.exit((int) exitCode);
    return RuntimeNull.NULL; 
  } 

  @NativeFunction
  public RuntimeInstance currentFiber(Fiber fiber, RuntimeInstance self, RuntimeInternalCallable callable, ArgVector args) throws InvocationException{
    return fiber;
  } 

  @Override
  public String getName() {
    return SYSTEM_NAME;
  }
  
  public RuntimeModule getRuntimeModule() {
    return runtimeModule;
  }
  
  @NativeModuleDiscovery
  public static NativeModule getNativeModule() {
    return runtimeModule_INSTANCE;
  }
}
