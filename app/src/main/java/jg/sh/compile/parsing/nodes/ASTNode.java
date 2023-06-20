package jg.sh.compile.parsing.nodes;

/**
 * Represents a node on the AST, correlating to a line and column number
 * 
 * @author Jose
 *
 */
public abstract class ASTNode {

  private final int line;
  private final int column;

  public ASTNode(int line, int column) {
    this.line = line;
    this.column = column;
  }

  public int getLine() {
    return line;
  }
  
  public int getColumn() {
    return column;
  }
  
  public abstract void acceptVisitor(NodeVisitor visitor);
  
  public abstract boolean isLValue();
}
