package jg.sh.runtime.instrs;

import jg.sh.compile.instrs.OpCode;
import jg.sh.runtime.objects.RuntimeInstance;

public class ArgInstruction extends RuntimeInstruction {

  private final int argument;

  public ArgInstruction(OpCode opCode, int argument, int exceptionJumpIndex) {
    super(opCode, exceptionJumpIndex);
    this.argument = argument;

    if (OpCode.isANoArgInstr(opCode)) {
      throw new IllegalArgumentException(opCode+" expects no argument.");
    }
  }

  public boolean equals(Object obj) {
    if (obj instanceof ArgInstruction) {
      final ArgInstruction other = ((ArgInstruction) obj);
      return other.opCode == opCode && 
             other.exceptionJumpIndex == exceptionJumpIndex && 
             other.argument == argument;
    }
    return false;
  }
  
  public int getArgument() {
    return argument;
  }

  public final LoadedConstantInstruction cache(RuntimeInstance cacheTarget) {
    return new LoadedConstantInstruction(this, cacheTarget);
  }

  /**
   * Returns the cached RuntimeInstance associated with this instruction,
   * or null if nothing is cached.
   * @return the cached RuntimeInstance associated with this instruction,
   * or null if nothing is cached.
   */
  public RuntimeInstance getCache() {
    return null;
  }

  /**
   * Whether this ArgInstruction has an attached RuntimeInstance as its cache
   * @return true if there's a RuntimeInstance cached, false if else.
   */
  public boolean isCached() {
    return getCache() != null;
  }

  public String repr() {
    return opCode.name().toLowerCase()+" "+argument;
  }
}
