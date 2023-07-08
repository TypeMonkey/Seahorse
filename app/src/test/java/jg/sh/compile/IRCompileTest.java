package jg.sh.compile;

import static org.junit.jupiter.api.Assertions.fail;

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import jg.sh.parsing.Module;
import jg.sh.parsing.Parser;
import jg.sh.parsing.Tokenizer;
import jg.sh.parsing.exceptions.ParseException;
import jg.sh.util.StringUtils;

public class IRCompileTest {
  
  @Test
  public void testSimple() {
    try {
      final Module module = compileModule("multiFuncs.shr");
      final IRCompiler compiler = new IRCompiler();
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
    Parser parser = new Parser(tokenizer);

    return parser.parseProgram(StringUtils.getBareFileName(fileName));
  }

}
