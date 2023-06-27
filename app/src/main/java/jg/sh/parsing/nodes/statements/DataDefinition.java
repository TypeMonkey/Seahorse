package jg.sh.parsing.nodes.statements;

import java.util.LinkedHashMap;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.statements.blocks.Block;

/**
 * Represents a data type definition.
 * 
 * Format:
 * 
 * data [export] <dataTypeName {
 *  
 *   [ constr([parameter, ..... ]) {
 *       statements....
 *     }
 *   ]
 *     
 *   (
 *      func methodName([parameter, ...]) {
 *          statements....
 *      }
 *   )*
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
public class DataDefinition extends Statement {

  private final Identifier typeName;
  private final LinkedHashMap<Identifier, FuncDef> methods;
  private final boolean toExport;

  public DataDefinition(Identifier typeName, 
                        FuncDef constructor,
                        LinkedHashMap<Identifier, FuncDef> methods,
                        boolean toExport, 
                        Location end) {
    super(typeName.start, end);
    this.typeName = typeName;
    this.methods = methods;
    this.toExport = toExport;
  }
  
  public Identifier getName() {
    return typeName;
  }

  public LinkedHashMap<Identifier, FuncDef> getMethods() {
    return methods;
  }

  public boolean toExport() {
    return toExport;
  }
}
