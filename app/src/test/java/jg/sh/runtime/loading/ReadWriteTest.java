package jg.sh.runtime.loading;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.io.Reader;

import org.junit.jupiter.api.Test;

import jg.sh.InterpreterOptions;
import jg.sh.compile.CompilerResult;
import jg.sh.compile.IRCompiler;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.parsing.Module;
import jg.sh.parsing.Parser;
import jg.sh.parsing.Tokenizer;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.util.StringUtils;

public class ReadWriteTest {
  
  @Test
  public void testSimpleModule() {
    try {
      final Module module = compileModule("multiFuncs.shr");
      final IRCompiler compiler = new IRCompiler();
      final CompilerResult objectFile = compiler.compileModule(module);

      final HeapAllocator allocator = new HeapAllocator(0);
      ModuleFinder finder = new ModuleFinder(allocator, 
                                             new SeahorseCompiler(false), 
                                             InterpreterOptions.getDefaultOptions());

      finder.registerModule(objectFile.getObjectFile());

      final RuntimeModule runtimeModule = finder.getModule(module.getName());
      assertNotNull(runtimeModule);

      final byte [] modBytes = IRWriter.encodeModule(runtimeModule);
      final RuntimeModule readModule = IRReader.loadFromSHRCFile(allocator, runtimeModule.getName(), modBytes);

      assertEquals(runtimeModule.getName(), readModule.getName());
      assertEquals(runtimeModule.getConstants().length, readModule.getConstants().length);

      for (int i = 0; i < runtimeModule.getConstants().length; i++) {
        final RuntimeInstance instance = readModule.getConstant(i);
        final RuntimeInstance expected = runtimeModule.getConstant(i);

        assertInstanceOf(expected.getClass(), instance);
        assertEquals(expected, instance, expected.getClass()+" is equals to? "+instance.getClass());
      }
    } catch (Exception e) {
      fail(e);
    }
  }

  public Module compileModule(String fileName) throws ParseException {
    Reader src = new InputStreamReader(ReadWriteTest.class.getResourceAsStream("/"+fileName));

    Tokenizer tokenizer = new Tokenizer(src, false);
    Parser parser = new Parser(tokenizer, StringUtils.getBareFileName(fileName));

    return parser.parseProgram();
  }
}
