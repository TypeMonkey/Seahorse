package jg.sh.compile.instrs;

/**
 * A LoadCellInstr and StoreCellInstr pair.
 */
public class LoadStorePair {

  public final LoadCellInstr load;
  public final StoreCellInstr store;

  public LoadStorePair(LoadCellInstr load, StoreCellInstr store) {
    this.load = load;
    this.store = store;
  }
}
