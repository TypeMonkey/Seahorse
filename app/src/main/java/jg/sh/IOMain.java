package jg.sh;

import java.io.InputStreamReader;
import java.io.Reader;

import jg.sh.compile.CompilerResult;
import jg.sh.compile.IRCompiler;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.parsing.Module;
import jg.sh.parsing.Parser;
import jg.sh.parsing.Tokenizer;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.IRReader;
import jg.sh.runtime.loading.IRWriter;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.util.StringUtils;

public class IOMain {
  
  public static void main(String[] args) throws Exception {
    try {
      testSimpleModule();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void testSimpleModule() throws Exception {
    try {
      final Module module = compileModule("complexFuncs.shr");
      final IRCompiler compiler = new IRCompiler();
      final CompilerResult objectFile = compiler.compileModule(module);

      System.out.println(objectFile.getObjectFile());

      final HeapAllocator allocator = new HeapAllocator(0);
      ModuleFinder finder = new ModuleFinder(allocator, 
                                             new SeahorseCompiler(false), 
                                             InterpreterOptions.getDefaultOptions());

      finder.registerModule(objectFile.getObjectFile());

      final RuntimeModule runtimeModule = finder.getModule(module.getName());
      if (runtimeModule == null) {
        throw new RuntimeException("module is null");
      }

      final byte [] modBytes = IRWriter.encodeModule(runtimeModule);
      final RuntimeModule readModule = IRReader.loadFromSHRCFile(allocator, runtimeModule.getName(), modBytes);
    } catch (Exception e) {
      throw e;
    }
  }

  public static Module compileModule(String fileName) throws ParseException {
    Reader src = new InputStreamReader(IOMain.class.getResourceAsStream("/"+fileName));

    Tokenizer tokenizer = new Tokenizer(src, false);
    Parser parser = new Parser(tokenizer, StringUtils.getBareFileName(fileName));

    return parser.parseProgram();
  }
}
