package jg.sh.compile.instrs;

import jg.sh.common.Location;

/**
 * The comment instruction is not a true instruction. 
 * 
 * It's completely ignore by the SeaHorse interpreter, but is
 * useful for bytecode level debugging
 * @author Jose
 *
 */
public class CommentInstr extends Instruction{

  private final String content;
  
  public CommentInstr(Location start, Location end, String content) {
    super(start, end, OpCode.COMMENT);
    this.content = content;
  }
  
  public CommentInstr(Location start, Location end) {
    this(start, end, "");
  }
  
  public CommentInstr(String content) {
    this(Location.DUMMY, Location.DUMMY, content);
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "   # "+content;
  }
  
}
