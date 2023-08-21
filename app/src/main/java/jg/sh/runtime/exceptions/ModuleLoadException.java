package jg.sh.runtime.exceptions;

/**
 * Thrown by ModuleFinder when module loading fails.
 * 
 * Module loading can occur in the following (but not limited to) scenarios:
 * - Module cannot be found in the working directory, standard library paths, and 
 *   module search paths
 * - Reading/Writing of module was interrupted, causing an IOException
 * - Module (in the form of a .shrc file) is malformed
 * - Module has syntax errors
 * - Incompability between current interpreter version and module version
 * @author Jose Guaro
 */
public class ModuleLoadException extends Exception {
  
  public ModuleLoadException(String moduleName, String message) {
    super("Couldn't load "+moduleName+": "+message);
  }

}
