package jg.sh.compile;

import jg.sh.parsing.Tokenizer;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.parsing.token.TokenType;
import jg.sh.util.StringUtils;
import jg.sh.compile.exceptions.InvalidModulesException;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.optimization.OptimizingIRCompiler;
import jg.sh.parsing.Module;
import jg.sh.parsing.Parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Front-end for compiling a Seahorse source file (.shr) into 
 * a Module representation, as well as into Seahorse bytecode.
 */
public class SeahorseCompiler {
  
  private final Parser parser;
  private final IRCompiler irCompiler;

  public SeahorseCompiler() {
    this.parser = new Parser(Collections.emptyList(), null);
    this.irCompiler = new OptimizingIRCompiler();
  }

  public List<Module> compile(List<String> sourceFiles) throws IllegalArgumentException, ParseException, IOException {
    return compile(sourceFiles.toArray(new String[sourceFiles.size()]));
  }

  public List<Module> compile(String ... sourceFiles) throws IllegalArgumentException, ParseException, IOException {
    final ArrayList<Module> modules = new ArrayList<>();

    for (String source : sourceFiles) {
      final Path sourcePath = Paths.get(source);

      if (!sourcePath.toString().endsWith(".shr")) {
        throw new IllegalArgumentException("Not a SeaHorse (.shr) file: "+sourcePath);
      }
      
      final String moduleName = StringUtils.getBareFileName(sourcePath.getFileName().toString());
      
      //expressionBuilder.setFileName(moduleName);
      
      //throw out files that have illegal names
      if (TokenType.isKeyword(moduleName)) {
        throw new IllegalArgumentException("The module name '"+moduleName+"' is a keyword and cannot be used!");
      }

      final BufferedReader sourceReader = new BufferedReader(new FileReader(sourcePath.toFile()));
      final Tokenizer tokenizer = new Tokenizer(sourceReader, false);

      final Module module = parser.reset(tokenizer, moduleName).parseProgram(); 

      //close reader
      sourceReader.close();
      
      modules.add(module);
    }

    return modules;
  }

  public List<ObjectFile> generateByteCode(List<Module> modules) throws InvalidModulesException {
    return generateByteCode(modules.toArray(new Module[modules.size()]));
  }

  public List<ObjectFile> generateByteCode(Module ... modules) throws InvalidModulesException {
    final ArrayList<ObjectFile> moduleResults = new ArrayList<>();
    final Map<String, List<ValidationException>> exceptionMap = new HashMap<>();

    for (Module module : modules) {
      CompilerResult result = irCompiler.compileModule(module);
      if (!result.isSuccessful()) {
        exceptionMap.put(module.getName(), result.getValidationExceptions());
      }
      
      moduleResults.add(result.getObjectFile());
    }

    if (!exceptionMap.isEmpty()) {
      throw new InvalidModulesException(exceptionMap);
    }

    return moduleResults;
  }
}
