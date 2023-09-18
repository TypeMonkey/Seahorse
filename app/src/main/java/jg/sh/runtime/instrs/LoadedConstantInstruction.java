package jg.sh.runtime.instrs;

import jg.sh.runtime.objects.RuntimeInstance;

/**
 * This is an optimized version of the LOADC instruction.
 * 
 * If a LOADC instruction of the same index is encountered more than once
 * in the same StackFrame, the Seahorse interpreter will convert the original
 * ArgInstruction into a LoadedConstantInstruction - effectively caching the actual
 * constant RuntimeInstance with the LOADC instruction.
 * 
 * This Instruction can be used with non-LOADC instructions also. As long as the
 * intended instruction relies on loading RuntimeInstances from the constant pool, then 
 * it's possible to speed up loading by converting such instructions using this class.
 */
public class LoadedConstantInstruction extends ArgInstruction {

  private final RuntimeInstance constant;

  public LoadedConstantInstruction(ArgInstruction origin, RuntimeInstance constant) {
    super(origin.getOpCode(), origin.getArgument(), origin.exceptionJumpIndex);
    this.constant = constant;
  }
  
  @Override
  public RuntimeInstance getCache() {
    return constant;
  }

  @Override
  public boolean isCached() {
    return true;
  }
}
