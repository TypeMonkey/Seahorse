package jg.sh.compile;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jg.sh.compile.optimization.OptimizingIRCompiler;
import jg.sh.parsing.Module;
import jg.sh.parsing.Parser;
import jg.sh.parsing.Tokenizer;
import jg.sh.parsing.exceptions.ParseException;
//import jg.sh.runtime.threading.frames.BytecodeDispatch;
import jg.sh.util.StringUtils;

public class IRCompileTest {

  /*
  @Test
  public void testBytecodeAlignment() {
    assertEquals(OpCode.values().length, BytecodeDispatch.getAll().length);
    
    for (int i = 0; i < OpCode.values().length; i++) {
      assertEquals(OpCode.values()[i], BytecodeDispatch.getAll()[i].getOp());
    }
  }
  */
  
  @Test
  public void testSimple() {
    try {
      final Module module = compileModule("multiFuncs.shr");
      final IRCompiler compiler = new OptimizingIRCompiler();
      CompilerResult objectFile = compiler.compileModule(module);

      if (objectFile.isSuccessful()) {
        System.out.println(objectFile.getObjectFile());
      }
      else {
        fail(objectFile.getValidationExceptions().stream()
                                                 .map(x -> x.getMessage())
                                                 .collect(Collectors.joining(System.lineSeparator())));
      }
    } catch (ParseException e) {
      fail(e);
    }
  }

  public Module compileModule(String fileName) throws ParseException {
    Reader src = new InputStreamReader(IRCompileTest.class.getResourceAsStream("/"+fileName));

    Tokenizer tokenizer = new Tokenizer(src, false);
    Parser parser = new Parser(tokenizer, StringUtils.getBareFileName(fileName));

    return parser.parseProgram();
  }

}
