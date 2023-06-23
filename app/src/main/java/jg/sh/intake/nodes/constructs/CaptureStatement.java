package jg.sh.intake.nodes.constructs;

import java.util.LinkedHashSet;

import jg.sh.intake.Location;

public class CaptureStatement extends Statement {

  private final LinkedHashSet<VariableDeclr> captures;

  protected CaptureStatement(Location start, Location end, LinkedHashSet<VariableDeclr> captures) {
    super(start, end);
    this.captures = captures;
  }
  
  public LinkedHashSet<VariableDeclr> getCaptures() {
    return captures;
  }

  @Override
  public String repr() {
    return "capture "+captures;
  }
}
