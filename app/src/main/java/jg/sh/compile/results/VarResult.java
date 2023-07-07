package jg.sh.compile.results;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadStorePair;
import jg.sh.parsing.nodes.Identifier;

/**
 * Utility class for passing information
 * about the load/store instructions of a variable.
 */
public class VarResult extends NodeResult {

  private final LinkedHashMap<Identifier, LoadStorePair> vars;

  public VarResult(List<ValidationException> exceptions, 
                    LinkedHashMap<Identifier, LoadStorePair> vars,
                    List<Instruction> instrs) {
    super(exceptions, instrs);  
    this.vars = vars;
  }

  public LinkedHashMap<Identifier, LoadStorePair> getVars() {
    return vars;
  }

  public static VarResult single(Identifier var, 
                                  LoadStorePair loadStore, 
                                  Instruction ... instrs) {
    LinkedHashMap<Identifier, LoadStorePair> map = new LinkedHashMap<>();
    map.put(var, loadStore);
    return new VarResult(Collections.emptyList(), map, Arrays.asList(instrs));
  }

  public static VarResult single(Identifier var, 
                                  LoadStorePair loadStore, 
                                  List<Instruction> instrs) {
    LinkedHashMap<Identifier, LoadStorePair> map = new LinkedHashMap<>();
    map.put(var, loadStore);
    return new VarResult(Collections.emptyList(), map, instrs);
  }
  }
