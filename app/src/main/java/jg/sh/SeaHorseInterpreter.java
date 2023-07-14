package jg.sh;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jg.sh.InterpreterOptions.IOption;
import jg.sh.compile.ObjectFile;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.compile.exceptions.InvalidModulesException;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.parsing.Module;
import jg.sh.runtime.alloc.CompactMarkSweepCleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.util.StringUtils;

public class SeaHorseInterpreter {
  
  public static final long VERSION = 1;
  
  /**
   * Folder - to be located in the current working directory - that 
   * stores bytecode transformation of modules (.shrc files)
   */
  public static final String CACHE_DIR_NAME = ".mod_cache/";
  
  private static final int MAX_GC_OBJECTS = 999999999;

  private final Map<IOption, Object> options;
  
  private final SeahorseCompiler seahorseCompiler;
  
  private final ThreadManager manager;
  private final HeapAllocator allocator;
  private final ModuleFinder finder;
  
  public SeaHorseInterpreter(Map<IOption, Object> options) {
    this.options = options;
    
    this.seahorseCompiler = new SeahorseCompiler();
    
    this.allocator = new HeapAllocator(MAX_GC_OBJECTS);
    this.finder = new ModuleFinder(allocator, seahorseCompiler, options);
    this.manager = new ThreadManager(allocator, finder, new CompactMarkSweepCleaner(), options);
  }
  
  /**
   * Initializes this interpreter for execution.
   * 
   * Tasks completed by this method includes the creation of a ".mod_cache" directory
   * in the current working directory if the COMP_TO_BYTE option is set to true.
   * 
   * @return true if all tasks were successfully completed, false if else.
   */
  public boolean init() {
    manager.initialize();
    
    if (options.containsKey(IOption.COMP_TO_BYTE) && (boolean) options.get(IOption.COMP_TO_BYTE)) {
      final File CACHE_DIR = new File(CACHE_DIR_NAME);
      System.out.println(" --> Making bytecode dir at: "+CACHE_DIR.getAbsolutePath()+" | "+CACHE_DIR.isDirectory());
      if (!CACHE_DIR.isDirectory()) {
        if (!CACHE_DIR.mkdir()) {
          return false;
        }
      }
    }
    
    return true;
  }
  
  /**
   * Starts the execution of the given module.
   * @param module - the path to the module's source file to execute
   * @param otherModules - other modules to prepare, but not necessarily execute unless loaded by the first module
   * 
   * Note: if the IOption.MEASURE is set to true, this method will block until all Fibers
   *       have been completed/terminated.
   */
  public void executeModule(String module, String [] args){        
    try {
      //Put all modules in one array. First module is the module to execute.
      final List<String> allModules = new ArrayList<>();
      allModules.add(module);

      final String [] otherModules = (String[]) options.get(IOption.ADDITIONAL);
      
      if (otherModules != null && otherModules.length > 0) {
        allModules.addAll(Arrays.asList(otherModules));
      }
      
      //List of compiled modules given successful compilation
      List<ObjectFile> compiledModules = null;

      //Now, parse all modules
      try {
        final List<Module> rawModules = seahorseCompiler.compile(allModules);
        compiledModules = seahorseCompiler.generateByteCode(rawModules);

        System.out.println(compiledModules.stream().map(ObjectFile::toString).collect(Collectors.joining(System.lineSeparator())));
      } catch (InvalidModulesException e) {
        /*
         * Print out (to stderr) all found errors. 
         */
        for (Entry<String, List<ValidationException>> res : e.getModuleExceptions().entrySet()) {
          System.err.println("Validation errors for '"+res.getKey()+"': ");
          for (ValidationException exception : res.getValue()) {
            System.err.println("  -> "+exception.getMessage());
          }
        }
        System.out.println("Interpreter exiting......");
        return;
      }
      
      /*
      if (options.containsKey(IOption.INTERPRET_ONLY) && ((boolean) options.get(IOption.INTERPRET_ONLY))) {
        System.out.println("Interpretation-ONLY mode currently not supported. Exiting.....");
      }
      else 
      */
      
      //Do code gen and then execution
      {
        //Call GC to hopefully clear out the pre-bytecode objects
        
        System.out.println("   *** PROFILE POINT: After Parsing --: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        System.gc();
        System.out.println("   *** PROFILE POINT: After Parsing GC: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
              
        //TODO: Needed so I can latch visualvm on this
        Thread.sleep(15000);

        finder.registerModules(compiledModules);
        try {
          RuntimeModule mainModule = finder.getModule(compiledModules.get(0).getName());
          manager.spinFiber((RuntimeCallable) mainModule.getModuleCallable(), new ArgVector());
          
          if (options.containsKey(IOption.MEASURE) && ((boolean) options.get(IOption.MEASURE))) {
            final long start = System.nanoTime();
            manager.start(true);
            final long end = System.nanoTime();
            System.out.println("Seahorse VM elasped time: "+(end - start)+" nanoseconds, or "+( (end-start) / 1000000)+" ms");
          }
          else{
            manager.start(false);
          }

        } catch (Exception e) {
          System.err.println("Exception encountered while initializing interpreter:");
          System.err.println(e.getMessage());
          e.printStackTrace();
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println(e.getMessage());
    }
  }
  
  public static void main(String[] args) throws Exception {
    System.out.println("---> In SeaHorseInterpreter "+System.getProperty("java.version")+" | "+System.getProperty("java.vm.name"));

    //Sanity line to make sure that args isn't null
    args = args == null ? new String[0] : args;
    
    System.out.println(new File("").getAbsolutePath());
    String mainModule = "../sampleSrcs/fibb_sync.shr";
    
    Map<IOption, Object> options = InterpreterOptions.getDefaultOptions();
    options.put(IOption.MEASURE, true);
    options.put(IOption.MODULE_SEARCH, StringUtils.wrap("../sampleSrcs"));
    options.put(IOption.POOL_SIZE, 2);
    
    SeaHorseInterpreter interpreter = new SeaHorseInterpreter(options);
    interpreter.init();
    interpreter.executeModule(mainModule, args);
  }
}
