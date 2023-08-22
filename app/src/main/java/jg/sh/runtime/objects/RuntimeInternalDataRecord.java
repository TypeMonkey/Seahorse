package jg.sh.runtime.objects;

import java.util.Collections;
import java.util.Map;

import jg.sh.runtime.objects.callable.InternalFunction;

public class RuntimeInternalDataRecord extends RuntimeDataRecord {

  private final Map<String, InternalFunction> methods;

  public RuntimeInternalDataRecord(String name, Map<String, InternalFunction> methods, boolean instancesSealed) {
    super(name, Collections.emptyMap(), instancesSealed);
    this.methods = methods;
  }
  
}
