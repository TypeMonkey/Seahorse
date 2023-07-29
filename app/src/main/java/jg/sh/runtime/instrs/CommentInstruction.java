package jg.sh.runtime.instrs;

import jg.sh.compile.instrs.OpCode;

public class CommentInstruction extends RuntimeInstruction {

  private final String comment;

  public CommentInstruction(int exceptionJumpIndex, String comment) {
    super(OpCode.COMMENT, exceptionJumpIndex);
    this.comment = comment;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public String repr() {
    return comment;
  }
}
