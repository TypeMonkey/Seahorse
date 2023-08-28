package jg.sh.runtime.loading;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.SeaHorseInterpreter;
import jg.sh.InterpreterOptions.IOption;
import jg.sh.common.FunctionSignature;
import jg.sh.modules.NativeFunction;
import jg.sh.modules.NativeModule;
import jg.sh.modules.NativeModuleDiscovery;
import jg.sh.modules.builtin.SystemModule;
import jg.sh.parsing.Module;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.alloc.Markable;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.ModuleLoadException;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.Initializer;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.InternalFunction;
import jg.sh.runtime.objects.callable.PervasiveFuncInterface;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.callable.RuntimeInternalCallable;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.compile.ObjectFile;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.compile.exceptions.InvalidModulesException;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.MutableIndex;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.CodeObject;
import jg.sh.compile.pool.component.DataRecord;
import jg.sh.compile.pool.component.ErrorHandlingRecord;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.pool.component.StringConstant;
import jg.sh.util.RuntimeUtils;

public class ModuleFinder implements Markable {

  private static Logger LOG = LogManager.getLogger(ModuleFinder.class);
    
  public static final Function<String, String> MODULE_START_LABEL_GEN = (modName) -> "$module_"+modName+"_start";

  private final HeapAllocator allocator;
  private final ByteBasedClassLoader classLoader;
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
    this.classLoader = new ByteBasedClassLoader();

    //Add "system" module
    modules.put(SystemModule.SYSTEM_NAME, prepareSystemModule());  
  }
  
  private RuntimeModule prepareSystemModule() {
    final NativeModule systemNativeModule = SystemModule.getNativeModule();
    final RuntimeModule systemModule = systemNativeModule.getModule();

    final InternalFunction nativeLoadingFunc = InternalFunction.create(FunctionSignature.NO_ARG, (fiber, self, callable, args) -> {
      systemNativeModule.initialize(self);
      return self;
    });
    
    final RuntimeInstance systemObject = allocator.allocateEmptyObject((o, m) -> {
      prepareFromAnnotations(systemNativeModule, o, m);
      systemNativeModule.initialAttrs(m, o);
    });
    
    final RuntimeInternalCallable initialization = new RuntimeInternalCallable(systemModule, systemObject, nativeLoadingFunc);
    systemModule.setLoadingComponents(systemObject, initialization);
        
    return systemModule;
  }
  
  public RuntimeModule load(String name) throws ModuleLoadException {
    //System.out.println(" ===> loading: "+name+" | "+modules.keySet());
    RuntimeModule module = modules.get(name);
    if (module == null) {                  

      final String [] allDirectories = Stream.of((String[]) options.getOrDefault(IOption.ST_LIB_PATH, IOption.ST_LIB_PATH.getDefault()),

                                                  Stream.of((String[]) options.getOrDefault(IOption.MODULE_SEARCH, IOption.MODULE_SEARCH.getDefault()))
                                                        .map(x -> new File(x).getAbsolutePath())
                                                        .toArray(String[]::new),

                                                  new String[]{new File(System.getProperty("user.dir")).toString()})

                                              .flatMap(Stream::of)
                                              .toArray(String[]::new);
      
      try {
        module = loadFromDirectories(allDirectories, name, false);
      } catch (Exception e) {
        throw new ModuleLoadException(name, e.getMessage());
      }     
    }
    
    if (module != null) {
      //Cache loaded module into our map
      modules.put(module.getName(), module);
      return module;
    }

    throw new ModuleLoadException(name, "Couldn't find module '"+name+"'");
  }
  
  private RuntimeModule loadFromDirectories(String [] directories, String moduleName, boolean considerClassFiles) throws Exception {
    //RuntimeModule module = null;
    for (String path : directories) {      
      final Path classFile = Paths.get(path, moduleName+".class");
      final Path shrFile = Paths.get(path, moduleName+".shr");
      //final File classFile = new File(path, moduleName+".class");
      //final File shrFile = new File(path, moduleName+".shr");
      
      if (Files.isReadable(shrFile)) {
        final Path shrcFile = Paths.get(SeaHorseInterpreter.CACHE_DIR_NAME, moduleName+".shrc");
        
        System.out.println(" ===> shrc file? "+shrcFile.toAbsolutePath().toString()+" || "+Files.isReadable(shrcFile));

        if (Files.isReadable(shrcFile)) {
          final Instant shrcLastModified = Files.getLastModifiedTime(shrcFile).toInstant();
          final Instant shrLastModified = Files.getLastModifiedTime(shrFile).toInstant();

          System.out.println(" ===> shrc vs shr lastModified "+shrcLastModified.compareTo(shrLastModified));

          if (shrcLastModified.compareTo(shrLastModified) > 0) {
            System.out.println("---- shrc file is newer, loading from shrc");

            //.shrc file is newer. use shrc!
            try {
              final RuntimeModule module = loadFromSHRC(shrcFile);

              final RuntimeInstance moduleObject = allocator.allocateEmptyObject();
              final RuntimeCallable initCallable = allocator.allocateCallable(module, 
                                                                              moduleObject, 
                                                                              module.getModuleCodeObject(), 
                                                                              new CellReference[0]);
              module.setLoadingComponents(moduleObject, initCallable);

              return module;
            } catch (Exception e) {
              System.out.println("Error encoutered while loading shrc file: "+e);
              e.printStackTrace(System.out);
            }
          }
        }

        System.out.println("---- Falling back to loading shr file (either because shrc failed or shr is newer)");

        final RuntimeModule module = loadFromSHRFile(shrFile);

        final RuntimeInstance moduleObject = allocator.allocateEmptyObject();
        final RuntimeCallable initCallable = allocator.allocateCallable(module, 
                                                                          moduleObject, 
                                                                          module.getModuleCodeObject(), 
                                                                          new CellReference[0]);
        module.setLoadingComponents(moduleObject, initCallable);

        if ((boolean) options.getOrDefault(IOption.COMP_TO_BYTE, IOption.COMP_TO_BYTE.getDefault())) {
          IRWriter.printCompiledFile(SeaHorseInterpreter.CACHE_DIR_NAME, module);
        }

        return module;
      }

      if (considerClassFiles && Files.isReadable(classFile)) {
        
        final NativeModule nativeModule = loadFromClassFile(classFile);
        if(nativeModule != null) {
          RuntimeInstance moduleObject = allocator.allocateEmptyObject((ini, self) -> {
            prepareFromAnnotations(nativeModule, ini, self);
            nativeModule.initialAttrs(self, ini);
          });
          
          final RuntimeModule module = nativeModule.getModule();
          final InternalFunction nativeLoadingFunc = InternalFunction.create(FunctionSignature.NO_ARG, (fiber, self, callable, args) -> {
            nativeModule.initialize(self);
            return self;
          });
          final RuntimeInternalCallable initialization = new RuntimeInternalCallable(module, moduleObject, nativeLoadingFunc);
          module.setLoadingComponents(moduleObject, initialization);
          return module;
        }
      }
    }
    
    throw new Exception("Couldn't determine module path.");
  }

  public void prepareFromAnnotations(NativeModule module, Initializer ini, RuntimeInstance moduleObject) {
    final Class<?> actualClass = module.getClass();

    /**
     * Link methods annotated with NativeFunction
     */
    for(Method method : actualClass.getDeclaredMethods()) {
      if (method.isAnnotationPresent(NativeFunction.class) && 
          method.getParameterCount() == 4 &&

          method.getParameterTypes()[0] == Fiber.class && 
          RuntimeInstance.class.isAssignableFrom(method.getParameterTypes()[1]) &&
          method.getParameterTypes()[2] == RuntimeInternalCallable.class && 
          method.getParameterTypes()[3] == ArgVector.class &&
          RuntimeInstance.class.isAssignableFrom(method.getReturnType())) {
        //Internal function paramters are: Fiber, self (RuntimeInstance), callable, argVector
        final NativeFunction annotation = method.getAnnotation(NativeFunction.class);
        final FunctionSignature signature = new FunctionSignature(annotation.positionalParams(), 
                                                                  new HashSet<>(Arrays.asList(annotation.optionalParams())), 
                                                                  annotation.hasVariableParams(),
                                                                  annotation.hasVarKeywordParams());

        final boolean isStatic = Modifier.isStatic(method.getModifiers());
        final String attrName = annotation.name().isEmpty() ? method.getName() : annotation.name();

        try {
          final MethodHandles.Lookup lookup = MethodHandles.lookup();
          final MethodHandle handle = lookup.unreflect(method);
          final MethodType type = handle.type();

          final MethodType factoryType = isStatic ? 
                                          MethodType.methodType(PervasiveFuncInterface.class) :
                                          MethodType.methodType(PervasiveFuncInterface.class, type.parameterType(0));

          final MethodType interMethType = isStatic ? 
                                            type.changeParameterType(1, RuntimeInstance.class) :
                                            type.dropParameterTypes(0, 1).changeParameterType(1, RuntimeInstance.class);

          final MethodType targetType = isStatic ? type : type.dropParameterTypes(0, 1);

          CallSite callSite = LambdaMetafactory.metafactory(lookup, 
                                                            "call", 
                                                            factoryType, 
                                                            interMethType, 
                                                            handle, 
                                                            targetType);
          
          PervasiveFuncInterface internal = (PervasiveFuncInterface) 
                                            (isStatic ? 
                                              callSite.getTarget().invoke() :
                                              callSite.getTarget().invoke(module));
          PervasiveFuncInterface filter = (f, self, callable, args) -> {
            try {
              return internal.call(f, (RuntimeInstance) method.getParameterTypes()[1].cast(self), callable, args);
            } catch (ClassCastException e) {
              throw new InvocationException("Expected "+method.getParameterTypes()[1].getName()+
                                            ", but was a "+self.getClass().getName(), callable);
            }
          };
          InternalFunction function = InternalFunction.create(signature, filter);
          ini.init(attrName, new RuntimeInternalCallable(module.getModule(), 
                                                                  moduleObject, 
                                                                  function));

        } catch (Throwable e) {
          throw new Error(e);
        }
      }
    }

  }
  
  /**
   * Loads a RuntimeModule from a .shrc file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .shrc file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   * @throws IOException
   * @throws IllegalArgumentException
   */
  private RuntimeModule loadFromSHRC(Path path) throws IllegalArgumentException, IOException {
    return IRReader.loadFromSHRCFile(allocator, path);
  }
  
  /**
   * Loads a RuntimeModule from a .class file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .class file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   * @throws IOException
   */
  private NativeModule loadFromClassFile(Path path) throws Exception {  
    //System.out.println("LOADING FROM CLASS "+path+" | "+path.isFile()+" | "+path.canRead()+" | "+path.getParent());

    final byte [] data = Files.readAllBytes(path);
      
    Class<?> targetClass = classLoader.loadClass(data);      
      
    return loadFromClass(targetClass);
  }

  public NativeModule loadFromClass(Class<?> targetClass) throws Exception {
    if (NativeModule.class.isAssignableFrom(targetClass)) {
      Method invocationTarget = null;

      /**
       * Search class for the static method on this class
       * to retrieve the RuntimeModule instance
       */
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
        try {
          return (NativeModule) invocationTarget.invoke(null);
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
          throw new Exception("Exception invoking retrieval method for class "+targetClass.getName()+"", e);
        }
      }
      
    }     

    throw new Exception("Java class "+targetClass.getName()+" isn't annotated as a NativeModule.");
  }
  
  /**
   * Loads a RuntimeModule from a .shr file
   * 
   * Note: the loadFromXXX() methods are meant to fail silently. Any exceptions
   *       encountered during the method will cause the return of a null.
   * 
   * @param path - the path to the .shr file
   * @return the RuntimeModule held by this .class file, or null if a RuntimeModule couldn't be loaded
   * @throws IOException
   * @throws ParseException
   * @throws IllegalArgumentException
   * @throws InvalidModulesException
   */
  private RuntimeModule loadFromSHRFile(Path path) throws IllegalArgumentException, 
                                                          ParseException, 
                                                          IOException, 
                                                          InvalidModulesException {
    final Module module = compiler.compile(path.toFile().toString()).get(0);
    final ObjectFile objectFile = compiler.generateByteCode(module).get(0); 
    final RuntimeModule runtimeModule = prepareModule(objectFile);
    return runtimeModule;
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
  public List<RuntimeModule> registerModules(List<ObjectFile> includedModules) {
    final ArrayList<RuntimeModule> mods = new ArrayList<>();

    for (ObjectFile compiledFile : includedModules) {
      mods.add(registerModule(compiledFile));
    }

    return mods;
  }
  
  /**
   * Registers a ObjectFile with this ModuleFinder.
   * 
   * This method will prepare a CompiledFile to a RuntimeModule. 
   * Registered modules are not loaded/executed. 
   * 
   * @param compiledFile - the CompiledFile to register
   */
  public RuntimeModule registerModule(ObjectFile compiledFile) {
    //System.out.println(compiledFile);
    //System.out.println("------------- "+compiledFile.getName());
    
    if (modules.containsKey(compiledFile.getName())) {
      throw new IllegalArgumentException("There's already a module called '"+compiledFile.getName()+"'");
    }
    
    final RuntimeModule preparedModule = prepareModule(compiledFile);
    final RuntimeInstance moduleObject = allocator.allocateEmptyObject();
    final RuntimeCallable initCallable = allocator.allocateCallable(preparedModule, 
                                                              moduleObject, 
                                                              preparedModule.getModuleCodeObject(), 
                                                              new CellReference[0]);
    preparedModule.setLoadingComponents(moduleObject, initCallable);
        
    modules.put(preparedModule.getName(), preparedModule);

    return preparedModule;
  }
  
  private RuntimeModule prepareModule(ObjectFile compiledFile) {    
    final RuntimeInstance [] constants = new RuntimeInstance[compiledFile.getConstants().size()];
    
    LinkedHashMap<Integer, CodeObject> codeObjects = allocateConstants(constants, compiledFile.getConstants());    
    
    final CodeObject moduleCodeObject = new CodeObject(
        FunctionSignature.NO_ARG, 
        MODULE_START_LABEL_GEN.apply(compiledFile.getName()),
        Collections.emptyMap(), 
        -1,-1,
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
    
    Map<Integer, RuntimeCodeObject> runtimeCodeObjects = contextualize(compiledFile.getConstants(), 
                                                                       constants,
                                                                       allInstructions, 
                                                                       codeObjectIndices, 
                                                                       codeObjects);

    compiledFile.getConstants()
                .stream()
                .filter(x -> x instanceof DataRecord)
                .map(x -> (DataRecord) x)
                .forEach(d -> {
                  final int index = d.getExactIndex();

                  final HashMap<String, RuntimeCodeObject> methods = new HashMap<>();
                  for (Entry<String, MutableIndex> mIndex : d.getMethods().entrySet()) {
                    methods.put(mIndex.getKey(), runtimeCodeObjects.get(mIndex.getValue().getIndex()));
                  }

                  constants[index] = allocator.allocateDataRecord(d.getName(), methods, d.isSealed());
                });
    
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
                                                    runtimeCodeObjects.get(-1), 
                                                    constants);
    return runtimeModule;
  }
  
  /**
   * Allocates non-code object constants
   * @param pool
   */
  private LinkedHashMap<Integer, CodeObject> allocateConstants(RuntimeInstance [] constantArray, List<PoolComponent> pool) {  
    LinkedHashMap<Integer, CodeObject> codeObjects = new LinkedHashMap<>();
    
    for (int i = 0; i < pool.size(); i++) {
      PoolComponent component = pool.get(i);
      if (component instanceof BoolConstant) {
        constantArray[i] = allocator.allocateBool(((BoolConstant) component).getValue());
        //constantMap.put(i, allocator.allocateBool(((BoolConstant) component).getValue()));
      }
      else if (component instanceof FloatConstant) {
        constantArray[i] = allocator.allocateFloat(((FloatConstant) component).getValue());
        //constantMap.put(i, allocator.allocateFloat(((FloatConstant) component).getValue()));
      }
      else if (component instanceof IntegerConstant) {
        constantArray[i] = allocator.allocateInt(((IntegerConstant) component).getValue());
        //constantMap.put(i, allocator.allocateInt(((IntegerConstant) component).getValue()));
      }
      else if (component instanceof StringConstant) {
        constantArray[i] = allocator.allocateString(((StringConstant) component).getValue());
        //constantMap.put(i, allocator.allocateString(((StringConstant) component).getValue()));
      }
      else if (component instanceof CodeObject) {
        CodeObject codeObject = (CodeObject) component;
        codeObjects.put(i, codeObject);
      }
    }
        
    return codeObjects;
  }
  
  private Map<Integer, RuntimeCodeObject> contextualize(List<PoolComponent> pool, 
                                                RuntimeInstance [] constants,
                                                List<Instruction> rawInstrs, 
                                                int [][] codeObjectIndices, 
                                                LinkedHashMap<Integer, CodeObject> codeObjects) {
        
    //Temporary class for setting error label to instructions
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
    pool.stream()
        .filter(c -> c instanceof ErrorHandlingRecord)
        .map(c -> (ErrorHandlingRecord) c)
        .forEach(record -> {
          int startInstr = labelMap.get(record.getStartTryCatch());
          int endInstr = labelMap.get(record.getEndTryCatch());
          //System.out.println(" ============== "+startInstr+" <-> "+endInstr);
          
          for( ;startInstr <= endInstr; startInstr++) {
            tempContextInstrs.set(startInstr, 
                new TempContextInstr(record.getCatchLabel(), rawInstrs.get(startInstr)));
          }
        });
    
    HashMap<Integer, RuntimeCodeObject> runtimeCodeObjects = new HashMap<>();
    
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
      
      final RuntimeInstruction [] contextualInstrs = new RuntimeInstruction[codeObject.getInstrs().size()];
      
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
          final JumpInstr jumpInstr = (JumpInstr) instr.instr;
          if (codeObjectJumps.containsKey(jumpInstr.getTargetLabel())) {
            instr.instr = new IndexedJumpInstr(jumpInstr, codeObjectJumps.get(jumpInstr.getTargetLabel()));
          }
          else {
            throw new Error("Unknown error jump label: "+jumpInstr.getTargetLabel()+" | "+codeObjectJumps);
          }
        }
        
        contextualInstrs[i - startIndex] = RuntimeUtils.translate(instr.instr, errorJumpIndex);
      }
      
      RuntimeCodeObject runtimeCodeObject = allocator.allocateCodeObject(codeObject.getBoundName(), 
                                                                         codeObject.getSignature(), 
                                                                         codeObject.getKeywordIndexes(), 
                                                                         codeObject.getVarArgIndex(),
                                                                         codeObject.getKeywordVarArgIndex(),
                                                                         contextualInstrs, 
                                                                         codeObject.getCaptures());
      if (coEntry.getKey() > -1) {
        /*
         * The codeObject with index -1 is the module instructions, which we
         * don't add in our contants array
         */
        constants[coEntry.getKey()] = runtimeCodeObject;
      }
      runtimeCodeObjects.put(coEntry.getKey(),runtimeCodeObject);
      
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
