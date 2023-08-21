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
    COMP_TO_BYTE(true),
    
    /**
     * Whether modules should be compiled without any optimization.
     * 
     * Value of this option should be a boolean. Default is false
     * 
     * It's NOT recommended to set this option to be true unless for instrumentation purposes.
     * Optimized bytecode compilation and execution provides significant, performance critical improvements.
     */
    INTERPRET_ONLY(false),  
    
    /**
     * Whether a module's bytecode transformation should be loaded and favored over the source text.
     * 
     * Value of this option should be a boolean. Default is true.
     * 
     * It's NOT recommended to set this option to be false unless for instrumentation purposes.
     * Bytecode compilation and execution provided significant, performance critical improvements.
     */
    LOAD_FROM_BYTE(true),
    
    /**
     * Sets the list of directories to search for when loading modules.
     * 
     * Value of this option should be an array of strings representing directory paths. 
     * Default is a single path, which is the current working directory.
     * 
     * The SeaHorse loading order is to first search for a module within the standard library, as set
     * by ST_LIB_PATH.
     */
    MODULE_SEARCH(new String[]{System.getProperty("user.dir")}),
    
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
    ST_LIB_PATH(new String[]{System.getProperty("user.dir")}),
    
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
    ADDITIONAL(new String[0]),
    
    /**
     * Whether to print the amount of milliseconds for the given module to execute after 
     * module completes terminating - if ever.
     * 
     * Value of this option should be a boolean. Default is false.
     */
    MEASURE(false),  
    
    /**
     * Sets the amount of threads to be used by the interpreter when executing
     * fibers (SeaHorse's default line of execution.)
     * 
     * Value of this option should be an int. Default is 4.
     * 
     * Note: the main thread (actually a fiber) is executed using this thread pool
     */
    POOL_SIZE(4),

    /**
     * Sets the logging level for diagnostic output from the interpreter.
     * 
     * Valid of this option should be a String of the following options (following Log4j2 levels):
     * ALL, DEBUG, ERROR, FATAL, INFO, OFF, TRACE, WARN
     * 
     * The default is OFF
     */
    LOG_LEVEL("OFF");

    private final Object defaultValue;

    private IOption(Object defaultValue) {
      this.defaultValue = defaultValue;
    }

    public Object getDefault() {
      return defaultValue;
    }
  }
  
  private static final Map<IOption, Object> DEFAULTS = new EnumMap<>(IOption.class);
  
  static {    
    for (IOption option : IOption.values()) {
      DEFAULTS.put(option, option.getDefault());
    }
  }
  
  private InterpreterOptions() {}

  public static Map<IOption, Object> getDefaultOptions(){
    return new EnumMap<>(DEFAULTS);
  }
}
