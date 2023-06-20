package jg.sh.runtime.loading;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.SeaHorseInterpreter;
import jg.sh.InterpreterOptions.IOption;
import jg.sh.common.FunctionSignature;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.compile.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile.validation.FileValidationReport;
import jg.sh.compile.validation.Validator;
import jg.sh.irgen.CompiledFile;
import jg.sh.irgen.IRCompiler;
import jg.sh.irgen.IRWriter;
import jg.sh.irgen.instrs.Instruction;
import jg.sh.irgen.instrs.JumpInstr;
import jg.sh.irgen.instrs.LabelInstr;
import jg.sh.irgen.pool.ConstantPool;
import jg.sh.irgen.pool.component.BoolConstant;
import jg.sh.irgen.pool.component.CodeObject;
import jg.sh.irgen.pool.component.ErrorHandlingRecord;
import jg.sh.irgen.pool.component.FloatConstant;
import jg.sh.irgen.pool.component.IntegerConstant;
import jg.sh.irgen.pool.component.PoolComponent;
import jg.sh.irgen.pool.component.StringConstant;
import jg.sh.modules.NativeModule;
import jg.sh.modules.NativeModuleDiscovery;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeObject;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.util.StringUtils;

public class ModuleFinder implements Markable {
    
  private final HeapAllocator allocator;
  //private final Executor executor;
  private final Map<IOption, Object> options;
  private final SeahorseCompiler compiler;
  private final Map<String, RuntimeModule> modules;
  
  private int gcMark;
  
  //private URLClassLoader classLoader;
  
  public ModuleFinder(HeapAllocator allocator, SeahorseCompiler compiler, Map<IOption, Object> options) {
    this.allocator = allocator;
    this.options = options;
    this.modules = new HashMap<>();  
    this.compiler = compiler;
    //Add "system" module
    modules.put(SystemModule.SYSTEM_NAME, prepareSystemModule());  
  }
  
  private RuntimeModule prepareSystemModule() {
    NativeModule systemNativeModule = SystemModule.getNativeModule();
    RuntimeModule systemModule = systemNativeModule.getModule();
    
    RuntimeObject systemObject = allocator.allocateEmptyObject();
    
    RuntimeInternalCallable initialization = new RuntimeInternalCallable(systemModule, systemObject, systemNativeModule.getLoadingFunction());
    systemModule.setLoadingComponents(systemObject, initialization);
        
    return systemModule;
  }
  
  public RuntimeModule load(String name) {
    RuntimeModule module = modules.get(name);
    if (module == null) {                  
      
      //Search for module in the standard library paths first
      String [] standardLibPaths = options.containsKey(IOption.ST_LIB_PATH) ? 
                                       (String[]) options.get(IOption.ST_LIB_PATH) : 
                                       new String[0];
      module = loadFromDirectories(standardLibPaths, name, true);
      //System.out.println("searched MODULE_SEARCH. "+(module != null));
      
      
      //Next, search for modules in the MODULE_SEARCH paths
      if (module == null) {
        String [] moduleSearchPaths = options.containsKey(IOption.MODULE_SEARCH) ? 
                                          (String[]) options.get(IOption.MODULE_SEARCH) : 
                                          new String[0];
        module = loadFromDirectories(moduleSearchPaths, name, false);
        //System.out.println("searched MODULE_SEARCH. "+(module != null));
      }
      
      //Finally, search for modules in the current working directory
      if (module == null) {
        String [] cwdPAths = { new File(System.getProperty("user.dir")).toString() };
        module = loadFromDirectories(cwdPAths, name, false);
      }      
    }
    
    /*
    if (module != null) {   
      if (module.isLoaded()) {
        return module;
      }
            
      module.setAsLoaded(true);     
      loadingThread.run(module.getModuleCallable());
    }
    */
    
    return module;
  }
  
  private RuntimeModule loadFromDirectories(String [] directories, String moduleName, boolean considerClassFiles) {
    RuntimeModule module = null;
    for (String path : directories) {      
      File classFile = new File(path, moduleName+".class");
      File shrFile = new File(path, moduleName+".shr");
      
      if (shrFile.isFile() && shrFile.canRead()) {
        File shrcFile = new File(new File(path, SeaHorseInterpreter.CACHE_DIR_NAME), moduleName+".shrc");
        
        if (shrcFile.isFile() && shrcFile.canRead()) {
            final long shrcLastModified = shrcFile.lastModified();
            final long shrLastModified = shrFile.lastModified();
            
            if (shrLastModified > shrcLastModified) {
              //System.out.println("---- shr file is newer, loading from shr");
              
              //.shr file is newer. use shr!
              if((module = loadFromSHRFile(shrFile)) != null) {
                RuntimeObject moduleObject = allocator.allocateEmptyObject();
                RuntimeCallable initCallable = allocator.allocateCallable(module, moduleObject, module.getModuleCodeObject(), new CellReference[0]);
                module.setLoadingComponents(moduleObject, initCallable);
                //update shrc file!
                if (options.containsKey(IOption.COMP_TO_BYTE) && (boolean) options.get(IOption.COMP_TO_BYTE)) {
                  IRWriter.printCompiledFile(SeaHorseInterpreter.CACHE_DIR_NAME, module);
                }
                break;
              }
            }
            else {
              //.shr is older, use shrc.
              module = loadFromSHRC(shrcFile);
              
              //System.out.println("---- shr file is older, loading from shrc "+(module != null));
              
              if(module != null) {
                RuntimeObject moduleObject = allocator.allocateEmptyObject();
                RuntimeCallable initCallable = allocator.allocateCallable(module, moduleObject, module.getModuleCodeObject(), new CellReference[0]);
                module.setLoadingComponents(moduleObject, initCallable);
                //System.out.println("---successfully loaded from shrc");
                break;
              }
              else if((module = loadFromSHRFile(shrFile)) != null){
                //fall back to using shrFile if, for some reason, shrcFile couldn't be used
                RuntimeObject moduleObject = allocator.allocateEmptyObject();
                RuntimeCallable initCallable = allocator.allocateCallable(module, moduleObject, module.getModuleCodeObject(), new CellReference[0]);
                module.setLoadingComponents(moduleObject, initCallable);
                
                //update shrc file!
                if (options.containsKey(IOption.COMP_TO_BYTE) && (boolean) options.get(IOption.COMP_TO_BYTE)) {
                  IRWriter.printCompiledFile(SeaHorseInterpreter.CACHE_DIR_NAME, module);
                }
                
                break;
              }
            }
        } 
        else if((module = loadFromSHRFile(shrFile)) != null){
          RuntimeObject moduleObject = allocator.allocateEmptyObject();
          RuntimeCallable initCallable = allocator.allocateCallable(module, moduleObject, module.getModuleCodeObject(), new CellReference[0]);
          module.setLoadingComponents(moduleObject, initCallable);
          
          if (options.containsKey(IOption.COMP_TO_BYTE) && (boolean) options.get(IOption.COMP_TO_BYTE)) {
            IRWriter.printCompiledFile(SeaHorseInterpreter.CACHE_DIR_NAME, module);
          }
          
          break;
        }
      }
      if (considerClassFiles && 
          classFile.isFile() && 
          classFile.canRead()) {
        
        NativeModule nativeModule = loadFromClassFile(classFile);
        if(nativeModule != null) {
          RuntimeObject moduleObject = allocator.allocateEmptyObject();
          
          RuntimeInternalCallable initialization = new RuntimeInternalCallable(module, moduleObject, nativeModule.getLoadingFunction());
          module = nativeModule.getModule();
          module.setLoadingComponents(moduleObject, initialization);
          break;
        }
      }
    }
    
    return module;
  }
  
  /**
   * Loads a RuntimeModule from a .shrc file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .shrc file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   */
  private RuntimeModule loadFromSHRC(File path) {
    try {
      return IRReader.loadFromSHRCFile(allocator, path);
    } catch (Exception e) {
      //e.printStackTrace();
      return null;
    }
  }
  
  /**
   * Loads a RuntimeModule from a .class file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .class file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   */
  private NativeModule loadFromClassFile(File path){  
    try {
      //System.out.println("LOADING FROM CLASS "+path+" | "+path.isFile()+" | "+path.canRead()+" | "+path.getParent());
      
                        
      URL [] classFileURLArray = {path.getParentFile().toURI().toURL()};
      
      //TODO: Need to be careful about making a class loader per class. Monitor memory consumption
      URLClassLoader classLoader = new URLClassLoader(classFileURLArray);
      
      Class<?> targetClass = Class.forName(StringUtils.getBareFileName(path.getName()), true, classLoader);      
      
      NativeModule nativeModule = null;
      
      if (NativeModule.class.isAssignableFrom(targetClass)) {
        Method invocationTarget = null;
        for(Method method : targetClass.getDeclaredMethods()) {
          if (method.isAnnotationPresent(NativeModuleDiscovery.class) && 
              NativeModule.class.isAssignableFrom(method.getReturnType()) && 
              method.getParameterCount() == 0 && 
              Modifier.isStatic(method.getModifiers())) {
            invocationTarget = method;
            break;
          }
        }
                
        if (invocationTarget != null) {
          nativeModule = (NativeModule) invocationTarget.invoke(null);
        }
        
      }     
      
      return nativeModule;
    } catch (Exception e) {
      System.out.println("class loading exception");
      e.printStackTrace();
      return null;
    }
  }
  
  /**
   * Loads a RuntimeModule from a .shr file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .shr file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   */
  private RuntimeModule loadFromSHRFile(File path) {
    try {
      Module module = compiler.formSourceFiles(path.toString())[0];
      
      if (options.containsKey(IOption.VALIDATE) && ((boolean) options.get(IOption.VALIDATE))) {
        Validator validator = new Validator();
        FileValidationReport report = validator.validate(module).get(module.getName());
        
        if (report.getExceptions().size() > 0) {
          return null;
        }
      }
      
      IRCompiler irCompiler = new IRCompiler();
      CompiledFile compiledFile = irCompiler.compileModules(module)[0];
      
      RuntimeModule runtimeModule = prepareModule(compiledFile);
      return runtimeModule;
    } catch (Exception e) {
      System.out.println(e.getMessage());
      return null;
    }
  }
  
  /**
   * Retrieves - and doesn't attempt to load - a module from this ModuleFinder's
   * module map
   * @param name - the name of this module
   * @return the RuntimeModule, or null if it's not found
   */
  public RuntimeModule getModule(String name) {
    return modules.get(name);
  }
    
  /**
   * Registers multiple CompiledFiles with this ModuleFinder
   * @param includedModules - the CompiledFiles to register
   */
  public void registerModules(CompiledFile ... includedModules) {
    for (CompiledFile compiledFile : includedModules) {
      registerModule(compiledFile);
    }
  }
  
  /**
   * Registers a CompiledFile with this ModuleFinder.
   * 
   * This method will prepare a CompiledFile to a RuntimeModule. 
   * Registered modules are not loaded/executed. 
   * 
   * @param compiledFile - the CompiledFile to register
   */
  public void registerModule(CompiledFile compiledFile) {
    //System.out.println(compiledFile);
    //System.out.println("------------- "+compiledFile.getName());
    
    if (modules.containsKey(compiledFile.getName())) {
      throw new IllegalArgumentException("There's already a module called '"+compiledFile.getName()+"'");
    }
    
    RuntimeModule preparedModule = prepareModule(compiledFile);
    
    RuntimeObject moduleObject = allocator.allocateEmptyObject();
    RuntimeCallable initCallable = allocator.allocateCallable(preparedModule, moduleObject, preparedModule.getModuleCodeObject(), new CellReference[0]);
    preparedModule.setLoadingComponents(moduleObject, initCallable);
        
    modules.put(preparedModule.getName(), preparedModule);
  }
  
  private RuntimeModule prepareModule(CompiledFile compiledFile) {
    HashMap<Integer, RuntimeInstance> constantMap = new HashMap<>();
    
    LinkedHashMap<Integer, CodeObject> codeObjects = allocateConstants(constantMap, compiledFile.getPool());    
    
    final CodeObject moduleCodeObject = new CodeObject(
        new FunctionSignature(new HashSet<>(), 0), 
        "$module_"+compiledFile.getName()+"_start",
        new HashMap<>(), 
        compiledFile.getModuleInstrs(), 
        new int[0]);
    codeObjects.put(-1, moduleCodeObject);
    
    int [][] codeObjectIndices = new int[codeObjects.size()][2];
        
    //Combine all instructions in one master list
    ArrayList<Instruction> allInstructions = new ArrayList<>();
    
    int startIndex = 0;
    int codeObjectIndex = 0;
    for(CodeObject codeObject : codeObjects.values()) {
      
      //System.out.println(" -> CODE OBJECT: "+codeObjectIndex);
      //System.out.println(codeObject);
      
      allInstructions.addAll(codeObject.getInstrs());
      
      codeObjectIndices[codeObjectIndex][0] = startIndex;
      startIndex += codeObject.getInstrs().size();
      codeObjectIndices[codeObjectIndex][1] = startIndex - 1;
      
      codeObjectIndex++;
    }
    
    //System.out.println("-----------------------------");
    
    List<RuntimeCodeObject> runtimeCodeObjects = contextualize(compiledFile.getPool(), 
                                                               constantMap,
                                                               allInstructions, 
                                                               codeObjectIndices, 
                                                               codeObjects);
    
    /*
    for (int i = 0; i < runtimeCodeObjects.size(); i++) {
      System.out.println(" ----> RUNTIME CODE OBJECT: "+i);
      final RuntimeCodeObject codeObject = runtimeCodeObjects.get(i);
      for(int c = 0; c < codeObject.getInstrs().length; c++) {
        System.out.println("   ["+StringUtils.leftPadStr(c, ' ', 2)+"]  "+codeObject.getInstrs()[c]);
      }
    }
    */    
    RuntimeModule runtimeModule = new RuntimeModule(compiledFile.getName(), 
                                                    runtimeCodeObjects.get(runtimeCodeObjects.size() - 1), 
                                                    constantMap);
    return runtimeModule;
  }
  
  /**
   * Allocates non-code object constants
   * @param pool
   */
  private LinkedHashMap<Integer, CodeObject> allocateConstants(Map<Integer, RuntimeInstance> constantMap,  ConstantPool pool) {  
    LinkedHashMap<Integer, CodeObject> codeObjects = new LinkedHashMap<>();
    
    for (int i = 0; i < pool.getPoolSize(); i++) {
      PoolComponent component = pool.getComponent(i);
      if (component instanceof BoolConstant) {
        constantMap.put(i, allocator.allocateBool(((BoolConstant) component).getValue()));
      }
      else if (component instanceof FloatConstant) {
        constantMap.put(i, allocator.allocateFloat(((FloatConstant) component).getValue()));
      }
      else if (component instanceof IntegerConstant) {
        constantMap.put(i, allocator.allocateInt(((IntegerConstant) component).getValue()));
      }
      else if (component instanceof StringConstant) {
        constantMap.put(i, allocator.allocateString(((StringConstant) component).getValue()));
      }
      else if (component instanceof CodeObject) {
        CodeObject codeObject = (CodeObject) component;
        
        codeObjects.put(i, codeObject);
      }
    }
        
    return codeObjects;
  }
  
  private List<RuntimeCodeObject> contextualize(ConstantPool pool, 
                                              Map<Integer, RuntimeInstance> constantMap,
                                              List<Instruction> rawInstrs, 
                                              int [][] codeObjectIndices, 
                                              LinkedHashMap<Integer, CodeObject> codeObjects){
        
    //Temporary class for typing error label to instructions
    class TempContextInstr {
      private final String errorLabel;
      private Instruction instr;
      
      public TempContextInstr(String errorLabel, Instruction instr) {
        this.errorLabel = errorLabel;
        this.instr = instr;
      }
    }
    
    //Array that holds temporary contextual instructions
    ArrayList<TempContextInstr> tempContextInstrs = new ArrayList<>();
    rawInstrs.forEach(x -> tempContextInstrs.add(new TempContextInstr(null, x)));
        
    HashMap<String, Integer> labelMap = new HashMap<>();
    
    //Map label to indexes
    for (int i = 0; i < rawInstrs.size(); i++) {
      if (rawInstrs.get(i) instanceof LabelInstr) {
        labelMap.put(((LabelInstr) rawInstrs.get(i)).getName(), i);
      }
    }
    
    //Set error jump labels
    for(ErrorHandlingRecord record : pool.getErrorHandlingRecords()) {
      int startInstr = labelMap.get(record.getStartTryCatch());
      int endInstr = labelMap.get(record.getEndTryCatch());
      
      //System.out.println(" ============== "+startInstr+" <-> "+endInstr);

      
      for( ;startInstr <= endInstr; startInstr++) {
        tempContextInstrs.set(startInstr, 
            new TempContextInstr(record.getCatchLabel(), rawInstrs.get(startInstr)));
      }
    }
    
    ArrayList<RuntimeCodeObject> runtimeCodeObjects = new ArrayList<>();
    
    //Now, set indices for jumps
    int codeObjIndex = 0;
    for(Entry<Integer, CodeObject> coEntry : codeObjects.entrySet() ) {
      HashMap<String, Integer> codeObjectJumps = new HashMap<>();
      
      final CodeObject codeObject = coEntry.getValue();
      
      final int startIndex = codeObjectIndices[codeObjIndex][0];
      final int endIndex = codeObjectIndices[codeObjIndex][1];
      
      for(int i = startIndex; i <= endIndex; i++) {
        TempContextInstr instr = tempContextInstrs.get(i);
        
        if (instr.instr instanceof LabelInstr) {
          LabelInstr labelInstr = (LabelInstr) instr.instr;
          codeObjectJumps.put(labelInstr.getName(), i - startIndex);
        }
      }
      
      final ContextualInstr [] contextualInstrs = new ContextualInstr[codeObject.getInstrs().size()];
      
      for(int i = startIndex; i <= endIndex; i++) {
        TempContextInstr instr = tempContextInstrs.get(i);
        
        int errorJumpIndex = -1;
        if (instr.errorLabel == null) {
          errorJumpIndex = -1;
        }
        else if (codeObjectJumps.containsKey(instr.errorLabel)) {
          errorJumpIndex = codeObjectJumps.get(instr.errorLabel);
        }
        else {
          throw new Error("Unknown error jump label: "+instr.errorLabel+" | "+codeObjectJumps);
        }
        
        if (instr.instr instanceof JumpInstr) {
          JumpInstr jumpInstr = (JumpInstr) instr.instr;
          if (codeObjectJumps.containsKey(jumpInstr.getTargetLabel())) {
            instr.instr = new IndexedJumpInstr(jumpInstr, codeObjectJumps.get(jumpInstr.getTargetLabel()));
          }
          else {
            throw new Error("Unknown error jump label: "+jumpInstr.getTargetLabel()+" | "+codeObjectJumps);
          }
        }
        
        contextualInstrs[i - startIndex] = new ContextualInstr(instr.instr, errorJumpIndex);
      }
      
      RuntimeCodeObject runtimeCodeObject = allocator.allocateCodeObject(codeObject.getBoundName(), 
                                                                         codeObject.getSignature(), 
                                                                         codeObject.getKeywordIndexes(), 
                                                                         contextualInstrs, 
                                                                         codeObject.getCaptures());
      constantMap.put(coEntry.getKey(), runtimeCodeObject);
      runtimeCodeObjects.add(runtimeCodeObject);
      
      codeObjIndex++;
    }
    
    return runtimeCodeObjects;
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
    for (RuntimeModule runtimeModule : modules.values()) {
      cleaner.gcMarkObject(runtimeModule);
    }
  }
  
}
