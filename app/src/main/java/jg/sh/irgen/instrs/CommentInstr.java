package jg.sh.irgen.instrs;

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
  
  public CommentInstr(int line, int col, String content) {
    super(line, col, OpCode.COMMENT);
    this.content = content;
  }
  
  public CommentInstr(int line, int col) {
    this(line, col, "");
  }
  
  public CommentInstr(String content) {
    this(-1, -1, content);
  }

  public String getContent() {
    return content;
  }

  @Override
  public String toString() {
    return "   # "+content;
  }
  
}
