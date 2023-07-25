package jg.sh.compile.results;

import java.util.List;

import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.pool.ConstantPool.MutableIndex;
import jg.sh.compile.pool.component.CodeObject;

public class FuncResult extends NodeResult {

  private final MutableIndex codeObjectIndex;

  public FuncResult(List<ValidationException> exceptions, 
                    List<Instruction> instructions, 
                    MutableIndex codeObjectIndex) {
    super(exceptions, instructions);
    this.codeObjectIndex = codeObjectIndex;
  }
  
  public MutableIndex getCodeObjectIndex() {
    return codeObjectIndex;
  }
}
