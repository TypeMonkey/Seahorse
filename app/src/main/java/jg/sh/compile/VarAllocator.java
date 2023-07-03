package jg.sh.compile;

import jg.sh.common.Location;
import jg.sh.compile.instrs.LoadStorePair;

@FunctionalInterface
public interface VarAllocator {
  
  public LoadStorePair generate(String name, Location start, Location end);

}
