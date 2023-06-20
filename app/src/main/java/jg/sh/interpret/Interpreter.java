package jg.sh.interpret;

import java.util.List;
import java.util.Map;

import jg.sh.compile.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.Statement;
import jg.sh.interpret.alloc.CallStack;
import jg.sh.interpret.alloc.Heap;
import jg.sh.interpret.objects.ModuleInstance;

public class Interpreter {

  private final Heap heap;
  private final CallStack stack;
  private final Map<String, ModuleInstance> visibleModules;
  
  public Interpreter(Heap heap, CallStack stack, Map<String, ModuleInstance> visibleModules) {
    this.heap = heap;
    this.stack = stack;
    this.visibleModules = visibleModules;
  }
  
  public void execute(Map<String, Module> sourceModules, String moduleToExecute) {
    
  }
  
  public int executeFunction(List<Statement> statements) {
    return 0;
  }
  
  public Heap getHeap() {
    return heap;
  }
  
  public CallStack getStack() {
    return stack;
  }
  
  
}
