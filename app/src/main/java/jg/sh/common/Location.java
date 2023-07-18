package jg.sh.common;

/**
 * Line and column number location,
 * meant to signify the source location of a Node
 */
public class Location {

  public static final Location DUMMY = new Location(-1, -1);

  public final int line;
  public final int column;

  public Location(int line, int column) {
    this.line = line;
    this.column = column;
  }

  @Override
  public String toString() {
    return toString(line, column);
  }

  /**
   * Formats the provided line and column as the following string:
   * "(ln <line_number>, col <col_number>)"
   * @param line - the line number
   * @param column - the column number
   * @return a String formatted as: "(ln <line_number>, col <col_number>)"
   */
  public static String toString(int line, int column) {
    return "(ln " + line + ", col " + column + ")";
  }
}
