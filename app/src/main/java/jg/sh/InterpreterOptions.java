package jg.sh;

import java.util.EnumMap;
import java.util.Map;

public class InterpreterOptions {

  public static enum IOption {
    /**
     * Writes the compiled bytecode translation of modules
     * to disk - for future use.
     * 
     * Value of this option should be a boolean. Default is true
     * 
     * If true, each loaded module is compiled to bytecode - assuming it's bytecode transformation
     * hasn't already been loaded - and written to a folder called ".cache" in the working directory.
     * 
     * If false, the bytecode transformation of a module will not be written out.
     */
    COMP_TO_BYTE,
    
    /**
     * Whether modules should be compiled without any optimization.
     * 
     * Value of this option should be a boolean. Default is false
     * 
     * It's NOT recommended to set this option to be true unless for instrumentation purposes.
     * Optimized bytecode compilation and execution provides significant, performance critical improvements.
     */
    INTERPRET_ONLY,  
    
    /**
     * Whether a module's bytecode transformation should be loaded and favored over the source text.
     * 
     * Value of this option should be a boolean. Default is true.
     * 
     * It's NOT recommended to set this option to be false unless for instrumentation purposes.
     * Bytecode compilation and execution provided significant, performance critical improvements.
     */
    LOAD_FROM_BYTE,
    
    /**
     * Sets the list of directories to search for when loading modules.
     * 
     * Value of this option should be an array of strings representing directory paths. 
     * Default is a single path, which is the current working directory.
     * 
     * The SeaHorse loading order is to first search for a module within the standard library, as set
     * by ST_LIB_PATH.
     */
    MODULE_SEARCH,
    
    /**
     * Sets the list of directories to search for modules.
     * 
     * Value of this option should be an array of strings representing directory paths. 
     * Default is a single path, which is the current working directory.
     * 
     * TODO: Set a specific place to put standard library files in by default.
     * 
     * The SeaHorse loading order is to first search for a module within the directories listed by this option.
     */
    ST_LIB_PATH,
    
    /**
     * Sets whether modules should be validated prior to execution.
     * 
     * Value of this option should be a boolean. Default is true.
     * 
     * Validation checks a module for important errors, such as unfound variable references.
     */
    @Deprecated
    VALIDATE,
    
    /**
     * An additional set of modules to compile with the main module.
     * 
     * Value of this option should be a String []. Default is an empty String [].
     * 
     * If the interpreter isn't in "Interpret-Only" mode, then setting this option
     * is true is helpful if the main module has a known set of modules
     * that it directly depends on as they'll compiled to fast, linear bytecode
     * along with the main module. 
     * 
     * So, when the main module is loading these modules, no parsing and compilation
     * is done at runtime.
     */
    ADDITIONAL,
    
    /**
     * Whether to print the amount of milliseconds for the given module to execute after 
     * module completes terminating - if ever.
     * 
     * Value of this option should be a boolean. Default is false.
     */
    MEASURE,  
    
    /**
     * Sets the amount of threads to be used by the interpreter when executing
     * fibers (SeaHorse's default line of execution.)
     * 
     * Value of this option should be an int. Default is 4.
     * 
     * Note: the main thread (actually a fiber) is executed using this thread pool
     */
    POOL_SIZE,

    /**
     * Sets the logging level for diagnostic output from the interpreter.
     * 
     * Valid of this option should be a String of the following options (following Log4j2 levels):
     * ALL, DEBUG, ERROR, FATAL, INFO, OFF, TRACE, WARN
     * 
     * The default is OFF
     */
    LOG_LEVEL;
  }
  
  private static final Map<IOption, Object> DEFAULTS = new EnumMap<>(IOption.class);
  static {
    String [] moduleSearch = {System.getProperty("user.dir")};
    
    DEFAULTS.put(IOption.COMP_TO_BYTE, true);
    DEFAULTS.put(IOption.LOAD_FROM_BYTE, true);
    DEFAULTS.put(IOption.MODULE_SEARCH, moduleSearch);
    DEFAULTS.put(IOption.ST_LIB_PATH, moduleSearch);
    DEFAULTS.put(IOption.MEASURE, false);
    DEFAULTS.put(IOption.POOL_SIZE, 1);
    DEFAULTS.put(IOption.LOG_LEVEL, "OFF");
    DEFAULTS.put(IOption.INTERPRET_ONLY, false);
  }
  
  private InterpreterOptions() {}

  public static Map<IOption, Object> getDefaultOptions(){
    return new EnumMap<>(DEFAULTS);
  }
}
