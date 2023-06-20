package jg.sh.modules.builtin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;

import jg.sh.common.FunctionSignature;
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
import jg.sh.runtime.objects.literals.RuntimePrimitive;
import jg.sh.runtime.objects.literals.RuntimeString;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.FUNC_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.SELF_INDEX;


/**
 * Represents the "system" module.
 * 
 * This module houses a collection of important and useful functions and
 * values, ranging from functions dealing with I/O to arithmetic. 
 * @author Jose
 */
public class SystemModule extends NativeModule{
  public static final String SYSTEM_NAME = "system";
  
  private static SystemModule runtimeModule_INSTANCE;
  
  private final BufferedReader INPUT_READER;
  
  private SystemModule() {    
    super(SYSTEM_NAME);
    
    this.INPUT_READER = new BufferedReader(new InputStreamReader(System.in));
  }
  
  @Override
  public void initModule(RuntimeObject systemObject)  {    
    systemObject.setAttribute("print", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        System.out.print(args.getPositional(ARG_INDEX));
        return RuntimeNull.NULL;
      }
    )));

    systemObject.setAttribute("println", new RuntimeInternalCallable(runtimeModule, systemObject, create(new FunctionSignature(Collections.emptySet(), 0,  Collections.emptySet(), true), 
      (fiber, args) -> {
        for(int i = ARG_INDEX; i < args.getPositionals().size(); i++) {
          System.out.print(args.getPositional(i));
        }
        
        System.out.println();
        return RuntimeNull.NULL;
      }
    )));
    
    systemObject.setAttribute("bind", new RuntimeInternalCallable(runtimeModule, systemObject, create(new FunctionSignature(Collections.emptySet(), 2),
      (fiber, args) ->  {
        RuntimeInstance targetObject = args.getPositional(ARG_INDEX);
        RuntimeInstance targetFunction = args.getPositional(ARG_INDEX + 1);
        //System.out.println("  **** "+targetObject.getClass()+" , "+targetFunction.getClass());
        
        if (targetFunction instanceof Callable) {
          RuntimeCallable callable = (RuntimeCallable) targetFunction;
          return callable.rebind(targetObject, fiber.getHeapAllocator());
        }
        
        throw new InvocationException("Object provided isn't a callable", (Callable) args.getPositional(0));
      }
    )));
    systemObject.setAttribute("input", new RuntimeInternalCallable(runtimeModule, systemObject, create(new FunctionSignature(Collections.emptySet(), 0,  Collections.emptySet(), true), 
      (fiber, args) -> {
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
    )));

    systemObject.setAttribute("load", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
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
    )));

    systemObject.setAttribute("now", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.NO_ARG, 
      (fiber, args) -> {
        return fiber.getHeapAllocator().allocateInt(System.nanoTime());
      }
    )));

    systemObject.setAttribute("isArray", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimeArray);
      }
    )));

    systemObject.setAttribute("isFunction", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof Callable);
      }
    )));

    systemObject.setAttribute("isPrimitive", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimePrimitive);
      }
    )));
    
    systemObject.setAttribute("isError", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        return fiber.getHeapAllocator().allocateBool(args.getPositional(ARG_INDEX) instanceof RuntimeError);
      }
    )));
    
    systemObject.setAttribute("spinOff", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        RuntimeInstance argument = args.getPositional(ARG_INDEX); 
        if (argument instanceof Callable) {
          return fiber.getManager().spinFiber( (Callable) argument, new ArgVector());
        }
        else {
          throw new InvocationException("Callable expected", (Callable) args.getPositional(0));     
        }
      }
    )));

    systemObject.setAttribute("makeThread", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        RuntimeInstance argument = args.getPositional(ARG_INDEX); 
        if (argument instanceof Callable) {
          return fiber.getManager().makeThread((Callable) argument);
        }
         
        throw new InvocationException("Callable expected", (Callable) args.getPositional(0));
      }
    )));
    
    systemObject.setAttribute("toString", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.ONE_ARG, 
      (fiber, args) -> {
        RuntimeInstance argument = args.getPositional(ARG_INDEX); 
        return fiber.getHeapAllocator().allocateString(argument.toString()); 
      }
    )));
    
    systemObject.setAttribute("stop", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.NO_ARG, 
      (fiber, args) -> {
        fiber.getManager().stop();
        return RuntimeNull.NULL; 
      }
    )));

    systemObject.setAttribute("currentFiber", new RuntimeInternalCallable(runtimeModule, systemObject, create(FunctionSignature.NO_ARG, 
      (fiber, args) -> {
        return fiber;
      }
    )));
  }
  
  public RuntimeModule getRuntimeModule() {
    return runtimeModule;
  }
  
  @NativeModuleDiscovery
  public static NativeModule getNativeModule() {
    if (runtimeModule_INSTANCE == null) {
      runtimeModule_INSTANCE = new SystemModule();
    }
    return runtimeModule_INSTANCE;
  }
}
