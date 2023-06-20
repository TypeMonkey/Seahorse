package jg.sh.interpret.alloc;

import java.util.HashMap;
import java.util.Map;
import java.util.Stack;

public class CallStack {

  private final Stack<Map<String, Integer>> callStack;
  
  public CallStack() {
    this.callStack = new Stack<>();
  }
  
  public void createNewFrame() {
    callStack.push(new HashMap<>());
  }
  
  public void addVariable(String varName, int value) {
    callStack.peek().put(varName, value);
  }
  
  public int getVariable(String varName) {
    return callStack.peek().get(varName);
  }
  
  public void releaseFrame() {
    callStack.pop();
  }
  
  public int getFrameAmount() {
    return callStack.size();
  }
}
