package jg.sh.compile.instrs;

public interface MutableInstr {
  
  public void setIndex(int index);

  public int getIndex();

  public void setOpCode(OpCode op);

  public OpCode getOpCode();
}
