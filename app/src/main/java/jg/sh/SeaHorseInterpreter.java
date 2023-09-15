package jg.sh;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import jg.sh.InterpreterOptions.IOption;
import jg.sh.compile.ObjectFile;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.compile.exceptions.InvalidModulesException;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.parsing.Module;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.runtime.alloc.CompactMarkSweepCleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.metrics.GeneralMetrics;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.util.StringUtils;

public class SeaHorseInterpreter {

  private static Logger LOG = LogManager.getLogger(SeaHorseInterpreter.class);
  
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
   * in the current working directory if the COMP_TO_BYTE option is set to true,
   * as well as initializing the tread pool and logging.
   * 
   * @return true if all tasks were successfully completed, false if else.
   */
  public boolean init() {
    manager.initialize();
    
    System.out.println(" ===> Should create bytecode dir? "+options.get(IOption.COMP_TO_BYTE)+" | "+new File(CACHE_DIR_NAME).getAbsolutePath());
    if (options.containsKey(IOption.COMP_TO_BYTE) && (boolean) options.get(IOption.COMP_TO_BYTE)) {
      final File CACHE_DIR = new File(CACHE_DIR_NAME);
      System.out.println(" --> Making bytecode dir at: "+CACHE_DIR.getAbsolutePath()+" | "+CACHE_DIR.isDirectory());
      if (!CACHE_DIR.isDirectory()) {
        if (!CACHE_DIR.exists() && !CACHE_DIR.mkdir()) {
          return false;
        }
      }
    }

    final Level logLevel = Level.getLevel((String) options.getOrDefault(IOption.LOG_LEVEL, "OFF"));
    Configurator.setLevel(System.getProperty("log4j.logger"), logLevel == null ? Level.OFF : logLevel);    

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

      LOG.info(compiledModules.stream().map(ObjectFile::toString).collect(Collectors.joining(System.lineSeparator())));
    } catch (ParseException e) {
      System.err.println("Parsing exception! "+e.getMessage()+" at "+e.getModule()+".sh");
      LOG.debug("Parse exception: ", e);
      return;
    } catch (IOException e) {
      System.err.println("IO error has occured: "+e.getMessage());
      LOG.error("IO Error: ", e);
      return;
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
      LOG.info("Interpreter exiting......");
      return;
    }
    
    /*
    if (options.containsKey(IOption.INTERPRET_ONLY) && ((boolean) options.get(IOption.INTERPRET_ONLY))) {
      LOG.info("Interpretation-ONLY mode currently not supported. Exiting.....");
    }
    else 
    */
    
    //Do code gen and then execution
    {
      //Call GC to hopefully clear out the pre-bytecode objects
      
      LOG.info("   *** PROFILE POINT: After Parsing --: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
      System.gc();
      LOG.info("   *** PROFILE POINT: After Parsing GC: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
            
      /*
      //TODO: Needed so I can latch visualvm on this

      try {
        Thread.sleep(15000);
      } catch (InterruptedException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      */

      final RuntimeModule mainModule = finder.registerModules(compiledModules).get(0);

      //If there's a recursive reference to the main module, we need to make sure
      //that its properly recognized as loaded as to not re-initialize already initialized variables
      mainModule.setAsLoaded(true);
      try {
        manager.spinFiber((RuntimeCallable) mainModule.getModuleCallable(), new ArgVector());
      } catch (CallSiteException e) {
        System.err.println("-- Couldn't initiate main fiber! "+e.getMessage());
        e.printStackTrace();
      }
      
      if (options.containsKey(IOption.MEASURE) && ((boolean) options.get(IOption.MEASURE))) {
        final long start = System.currentTimeMillis();
        manager.start(true);
        final long end = System.currentTimeMillis();
        System.out.println("Seahorse VM elasped time: "+(end - start)+" ms");
      }
      else{
        manager.start(false);
      }
    }
  }
  
  public static void main(String[] args) throws Exception {
    LOG.info("---> In SeaHorseInterpreter "+System.getProperty("java.version")+" | "+System.getProperty("java.vm.name"));

    //Sanity line to make sure that args isn't null
    args = args == null ? new String[0] : args;
    
    LOG.info(new File("").getAbsolutePath());
    String mainModule = "../sampleSrcs/calling.shr";

    Map<IOption, Object> options = InterpreterOptions.getDefaultOptions();
    options.put(IOption.MEASURE, true);
    options.put(IOption.MODULE_SEARCH, StringUtils.wrap("../sampleSrcs"));
    options.put(IOption.POOL_SIZE, 1);
    options.put(IOption.LOG_LEVEL, "OFF");
    options.put(IOption.COMP_TO_BYTE, true);
    
    final long wholeStart = System.currentTimeMillis();
    SeaHorseInterpreter interpreter = new SeaHorseInterpreter(options);
    final boolean success = interpreter.init();
    System.out.println(" ==> was interpeter initialization successful? "+success);
    interpreter.executeModule(mainModule, args);
    final long wholeEnd = System.currentTimeMillis();
    System.out.println(" ===> Total Seahorse Runtime: "+(wholeEnd - wholeStart)+" ms");
    System.out.println(GeneralMetrics.statsAsStrings());
  }
}
