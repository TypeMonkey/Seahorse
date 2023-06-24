package jg.sh.parsing.nodes.statements;

import java.util.LinkedHashMap;

import jg.sh.common.Location;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.statements.blocks.Block;

/**
 * Represents a data type definition.
 * 
 * Format:
 * 
 * data [export] <dataTypeName> ([parameter, ....]) {
 *   statements....
 * }
 */
public class DataDefinition extends Statement {

  private final Identifier typeName;
  private final LinkedHashMap<String, Parameter> parameters;
  private final boolean toExport;
  private final Block body;

  public DataDefinition(Identifier typeName, 
                        LinkedHashMap<String, Parameter> parameters,
                        boolean toExport, 
                        Block body,
                        Location end) {
    super(typeName.start, end);
    this.typeName = typeName;
    this.parameters = parameters;
    this.body = body;
    this.toExport = toExport;
  }
  
  public Identifier getTypeName() {
    return typeName;
  }

  public LinkedHashMap<String, Parameter> getParameters() {
    return parameters;
  }

  public boolean toExport() {
    return toExport;
  }

  public Block getBody() {
    return body;
  }
}
