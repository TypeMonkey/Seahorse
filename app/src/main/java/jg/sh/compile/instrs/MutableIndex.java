package jg.sh.compile.instrs;

public class MutableIndex {

  private int index;

  public MutableIndex(int initialIndex) {
    this.index = initialIndex;
  }

  public MutableIndex() {
    this(-1);
  }

  public void setIndex(int index) {
    this.index = index;
  }

  public int getIndex() {
    return index;
  }

  public String toString() {
    return String.valueOf(index);
  }
}
