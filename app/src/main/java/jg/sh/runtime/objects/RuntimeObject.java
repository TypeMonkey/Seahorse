package jg.sh.runtime.objects;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jg.sh.common.FunctionSignature;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.exceptions.SealedObjectException;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;

import static jg.sh.runtime.objects.callable.InternalFunction.create;
import static jg.sh.runtime.objects.callable.InternalFunction.ARG_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.FUNC_INDEX;
import static jg.sh.runtime.objects.callable.InternalFunction.SELF_INDEX;

public class RuntimeObject extends RuntimeInstance {
    
  public static enum AttrModifier{
    CONSTANT;
  }

  private static final InternalFunction RETR_INDEX = create(FunctionSignature.ONE_ARG, 
    (fiber, args) -> {
      RuntimeObject self = (RuntimeObject) args.getPositional(SELF_INDEX);
      RuntimeInstance index = args.getPositional(ARG_INDEX);
      
      if (index instanceof RuntimeString) {
        RuntimeString string = (RuntimeString) index;
        return self.getAttr(string.getValue());
      }
      else if (index instanceof RuntimeInteger) {
        RuntimeInteger integer = (RuntimeInteger) index;
        return self.getAttr(String.valueOf(integer.getValue()));
      }
              
      throw new InvocationException("Unsupported index type '"+index+"'", (Callable) args.getPositional(FUNC_INDEX));
    }
  );

  private static final InternalFunction STORE_INDEX = create(FunctionSignature.ONE_ARG, 
    (fiber, args) -> {
      RuntimeObject self = (RuntimeObject) args.getPositional(SELF_INDEX);     
      RuntimeInstance index = args.getPositional(ARG_INDEX);
      RuntimeInstance newValue = args.getPositional(ARG_INDEX + 1);
      
      if (index instanceof RuntimeString) {
        RuntimeString string = (RuntimeString) index;
        self.setAttribute(string.getValue(), newValue);
        return RuntimeNull.NULL;
      }
      else if (index instanceof RuntimeInteger) {
        RuntimeInteger integer = (RuntimeInteger) index;
        self.setAttribute(String.valueOf(integer.getValue()), newValue);
        return RuntimeNull.NULL;
      }
      
      throw new InvocationException("Unsupported index type '"+index+"'", (Callable) args.getPositional(FUNC_INDEX));
    }
  );
  
  
  /**
   * Keeps track of which module variables are constant or not
   */
  private final Map<String, Set<AttrModifier>> attrDescriptions;
  
  public RuntimeObject() {
    super((self, m) -> {
      RuntimeModule systemModule = SystemModule.getNativeModule().getModule();
    
      m.put(RuntimeArray.STORE_INDEX_ATTR, new ImmediateInternalCallable(systemModule, self, STORE_INDEX));
      m.put(RuntimeArray.RETR_INDEX_ATTR, new ImmediateInternalCallable(systemModule, self, RETR_INDEX));
    });
    attrDescriptions = new HashMap<>();
  }
  
  public Set<AttrModifier> getAttrModifiers(String name) {
    return attrDescriptions.get(name);
  }
  
  @Override
  protected void markAdditional(Cleaner cleaner) {}
  
  public void setAttrModifiers(String name, AttrModifier ... modifiers) {
    if (attrDescriptions.containsKey(name)) {
      attrDescriptions.get(name).addAll(Arrays.asList(modifiers));
    }
    else {
      attrDescriptions.put(name, new HashSet<>(Arrays.asList(modifiers)));
    }
  }
}
