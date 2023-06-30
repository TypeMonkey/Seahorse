package jg.sh;

import java.io.File;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.InterpreterOptions.IOption;
import jg.sh.compile_old.CompilationException;
import jg.sh.compile_old.SeahorseCompiler;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile_old.validation.FileValidationReport;
import jg.sh.compile_old.validation.Validator;
import jg.sh.irgen.CompiledFile;
import jg.sh.irgen.IRCompiler;
import jg.sh.runtime.alloc.CompactMarkSweepCleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.threading.ThreadManager;
import jg.sh.util.StringUtils;
import net.percederberg.grammatica.parser.ParserCreationException;

public class SeaHorseInterpreter {
  
  public static final int VERSION = 1;
  
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
  
  public SeaHorseInterpreter(Map<IOption, Object> options) throws ParserCreationException {
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
      String [] otherModules = (String[]) options.get(IOption.ADDITIONAL);
      
      //Put all modules in one array. First module is the module to execute.
      String [] allModules = null;
      if (otherModules != null && otherModules.length > 0) {
        allModules = new String[1 + otherModules.length];
        allModules[0] = module;
        System.arraycopy(otherModules, 0, allModules, 1, otherModules.length);
      }
      else {
        allModules = new String[1];
        allModules[0] = module;
      }
      
      //Now, parse all modules
      Module [] rawModules = seahorseCompiler.formSourceFiles(allModules);
      if (options.containsKey(IOption.VALIDATE) && ((boolean) options.get(IOption.VALIDATE)) ) {
        Validator validator = new Validator();
        Map<String, FileValidationReport> reports = validator.validate(rawModules);
        
        /*
         * Checks if any of the modules had a validation exception
         */
        boolean errorFound = reports.values().stream().anyMatch(x -> x.getExceptions().size() > 0);
        
        if (errorFound) {
          for(Entry<String, FileValidationReport> report : reports.entrySet()) {
            System.err.println("Validation errors for '"+report.getKey()+"': ");
            for(CompilationException exception : report.getValue().getExceptions()) {
              System.err.println("  -> "+exception.getMessage());
              errorFound = true;
            }
          }
          System.out.println("Interpreter exiting.....");
          return;
        }
      }
      
      /*
      if (options.containsKey(IOption.INTERPRET_ONLY) && ((boolean) options.get(IOption.INTERPRET_ONLY))) {
        System.out.println("Interpretation-ONLY mode currently not supported. Exiting.....");
      }
      else 
      */
      
      //Do code gen and then execution
      {
        IRCompiler compiler = new IRCompiler();
        CompiledFile [] compiledFiles = compiler.compileModules(rawModules);
        
        //Call GC to hopefully clear out the pre-bytecode objects
        
        System.out.println("   *** PROFILE POINT: After Parsing --: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        System.gc();
        System.out.println("   *** PROFILE POINT: After Parsing GC: "+(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
              
        finder.registerModules(compiledFiles);
        try {
          RuntimeModule mainModule = finder.getModule(compiledFiles[0].getName());
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
