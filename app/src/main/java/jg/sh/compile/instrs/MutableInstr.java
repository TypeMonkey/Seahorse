package jg.sh.compile.instrs;

public interface MutableInstr {

  public void setOpCode(OpCode op);

  public OpCode getOpCode();
}
