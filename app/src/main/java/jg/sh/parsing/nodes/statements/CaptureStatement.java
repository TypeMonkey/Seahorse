package jg.sh.parsing.nodes.statements;

import java.util.Set;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.Identifier;

/**
 * Describes the captured variables of a function from 
 * a scope that inhabits it.
 * 
 * Format:
 * 
 * capture var1, ...
 * 
 * where varN is an identifier.
 * 
 * There should only be, at most, one capture statement inside a function, and it
 * should be the first statement.
 */
public class CaptureStatement extends Statement {

  private final Set<Identifier> captures;

  public CaptureStatement(Set<Identifier> captures, Location start, Location end) {
    super(start, end);
    this.captures = captures;
  }
  
  public Set<Identifier> getCaptures() {
    return captures;
  }
}
