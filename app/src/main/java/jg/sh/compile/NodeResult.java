package jg.sh.compile;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.Instruction;
import jg.sh.parsing.nodes.Node;

public class NodeResult {
  
  private final List<ValidationException> exceptions;
  private final Node target;
  private final List<Instruction> instructions;

  private NodeResult(List<ValidationException> exceptions, Node target, List<Instruction> instructions) {
    this.target = target;
    this.exceptions = exceptions;
    this.instructions = instructions;
  }

  public boolean hasExceptions() {
    return !exceptions.isEmpty();
  }

  public Node getTarget() {
    return target;
  }

  public List<ValidationException> getExceptions() {
    return exceptions;
  }

  public List<Instruction> getInstructions() {
    return instructions;
  }

  public static NodeResult invalid(Node target, ValidationException ... exceptions) {
    return new NodeResult(Arrays.asList(exceptions), target, Collections.emptyList());
  }

  public static NodeResult invalid(Node target, List<ValidationException> exceptions) {
    return new NodeResult(exceptions, target, Collections.emptyList());
  }

  public static NodeResult valid(Node target, Instruction ... exceptions) {
    return new NodeResult(Collections.emptyList(), target, Arrays.asList(exceptions));
  }

  public static NodeResult valid(Node target, List<Instruction> exceptions) {
    return new NodeResult(Collections.emptyList(), target, exceptions);
  }
}
