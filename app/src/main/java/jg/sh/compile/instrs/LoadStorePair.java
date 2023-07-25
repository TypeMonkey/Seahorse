package jg.sh.compile.instrs;

/**
 * A LoadCellInstr and StoreCellInstr pair.
 */
public class LoadStorePair {

  public final LoadInstr load;
  public final StoreInstr store;

  public LoadStorePair(LoadInstr load, StoreInstr store) {
    this.load = load;
    this.store = store;
  }
}
