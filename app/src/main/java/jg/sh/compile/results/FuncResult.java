package jg.sh.compile.results;

import java.util.List;

import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.pool.component.CodeObject;

public class FuncResult extends NodeResult {

  private final CodeObject codeObject;

  public FuncResult(List<ValidationException> exceptions, 
                    List<Instruction> instructions, 
                    CodeObject codeObject) {
    super(exceptions, instructions);
    this.codeObject = codeObject;
  }
  
  public CodeObject getCodeObject() {
    return codeObject;
  }
}
