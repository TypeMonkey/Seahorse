package jg.sh.runtime.loading;

import java.util.Map;

import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeObject;
import jg.sh.runtime.objects.callable.Callable;

public class RuntimeModule implements Markable {
  
  /*
  public static enum LoadingStatus {
    READ,
    LOADING,
    LOADED;
  }
  */
  
  private final String name;
  private final RuntimeCodeObject moduleCodeObject;
  private final Map<Integer, RuntimeInstance> constantMap;
  
  private Callable moduleCallable;
  private RuntimeObject moduleObject;
  private boolean isLoaded;
  
  private int gcMark;
  
  public RuntimeModule(String name, 
      RuntimeCodeObject codeObject,
      Map<Integer, RuntimeInstance> constantMap) {
    this.name = name;
    this.moduleCodeObject = codeObject;
    this.constantMap = constantMap;
  }
  
  public void setLoadingComponents(RuntimeObject moduleObject, Callable callable) {
    this.moduleCallable = callable;
    this.moduleObject = moduleObject;
  }
  
  public void setAsLoaded(boolean isLoaded) {
    this.isLoaded = isLoaded;
  }
  
  public boolean isLoaded() {
    return moduleObject != null && moduleCallable != null && isLoaded;
  }
  
  public Map<Integer, RuntimeInstance> getConstantMap() {
    return constantMap;
  }
  
  public RuntimeObject getModuleObject() {
    return moduleObject;
  }
  
  public Callable getModuleCallable() {
    return moduleCallable;
  }
  
  public RuntimeCodeObject getModuleCodeObject() {
    return moduleCodeObject;
  }
  
  public String getName() {
    return name;
  }
  
  @Override
  public void setGcFlag(int gcFlag) {
    this.gcMark = gcFlag;
  }
  
  @Override
  public int getGcFlag() {
    return gcMark;
  }

  @Override
  public void gcMark(Cleaner cleaner) {       
    if (moduleObject != null) {
      cleaner.gcMarkObject(moduleObject);
    }
    
    //Some modules don't have a code object - all NativeModules, as well as unloaded modules
    if (moduleCodeObject != null) {
      cleaner.gcMarkObject(moduleCodeObject);
    }
    cleaner.gcMarkObject(moduleCallable);
    
    for (RuntimeInstance value : constantMap.values()) {
      System.out.println("    ** marking constant for "+getName()+" : "+value);
      cleaner.gcMarkObject(value);
    }
  }
}
