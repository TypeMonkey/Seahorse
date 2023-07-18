package jg.sh.compile.exceptions;

import java.util.List;
import java.util.Map;

public class InvalidModulesException extends Exception {
  
  private final Map<String, List<ValidationException>> moduleExceptions;

  public InvalidModulesException(Map<String, List<ValidationException>> moduleExceptions) {
    this.moduleExceptions = moduleExceptions;
  }

  public Map<String, List<ValidationException>> getModuleExceptions() {
    return moduleExceptions;
  }
}
