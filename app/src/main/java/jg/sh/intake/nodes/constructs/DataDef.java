package jg.sh.intake.nodes.constructs;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.simple.Identifier;

/**
 * 
 * A data type definition.
 * 
 * Format:
 * 
 * data [export] <dataTypeName> ([parameter, ....]) {
 *   statements....
 * }
 * 
 * it's translated to:
 * 
 * func [export] <dataTypeName> ([parameter, ....]) {
 *    const obj := object sealed {
 *       parameter1 := parameter1; 
 *       .....
 *    };
 * 
 *    statements....
 *    
 *    return obj;
 * 
 * where "obj" is the "self" referred to in the original declaration
 */
public class DataDef extends Node {

  public DataDef(Identifier name, 
                 
                 Location start, 
                 Location end) {
    super(start, end);
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'accept'");
  }

  @Override
  public String repr() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'repr'");
  }

  @Override
  public boolean isLValue() {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'isLValue'");
  }
  
}
