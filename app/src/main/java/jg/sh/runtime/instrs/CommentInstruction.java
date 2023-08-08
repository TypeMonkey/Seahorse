package jg.sh.runtime.instrs;

import jg.sh.compile.instrs.OpCode;

public class CommentInstruction extends RuntimeInstruction {

  private final String comment;

  public CommentInstruction(int exceptionJumpIndex, String comment) {
    super(OpCode.COMMENT, exceptionJumpIndex);
    this.comment = comment;
  }

  public boolean equals(Object obj) {
    if (obj instanceof CommentInstruction) {
      final CommentInstruction other = ((CommentInstruction) obj);
      return other.opCode == opCode && 
             other.exceptionJumpIndex == exceptionJumpIndex && 
             other.comment.equals(comment);
    }
    return false;
  }

  public String getComment() {
    return comment;
  }

  @Override
  public String repr() {
    return comment;
  }
}
