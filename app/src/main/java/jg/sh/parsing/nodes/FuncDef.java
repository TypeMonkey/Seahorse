package jg.sh.parsing.nodes;

import java.util.LinkedHashMap;
import java.util.Set;

import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.Visitor;
import jg.sh.parsing.nodes.statements.blocks.Block;

/**
 * A function definition.
 * 
 * Top-level format:
 * 
 * func [export] <func_name> ([parameter,...]) {
 *    //function body
 * }
 * 
 * Expression-level format:
 * 
 * func [: <boundName>] ([parameter,...]) {
 *  //function body
 * }
 * 
 * Note: boundName is required if recursion is needed for that function
 * 
 * where the format of paramter is:
 *   parameter = identifier | identifier := expr
 * 
 * The first statement of a definition can be a capture statement - and only the first statement.
 */
public class FuncDef extends Node {

  private final Identifier boundName;
  private final FunctionSignature signature;
  private final Set<Identifier> captures;
  private final LinkedHashMap<String, Parameter> parameters;
  private final boolean toExport;
  private final Block body;

  public FuncDef(Identifier boundName, 
                 FunctionSignature signature,
                 Set<Identifier> captures, 
                 LinkedHashMap<String, Parameter> parameters,
                 boolean toExport, 
                 Block body,
                 Location start,
                 Location end) {
    super(start, end);
    this.boundName = boundName;
    this.signature = signature;
    this.captures = captures;
    this.parameters = parameters;
    this.body = body;
    this.toExport = toExport;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitFuncDef(parentContext, this);
  }

  @Override
  public String repr() {
    String r = "func "+(hasBoundName() ? boundName.getIdentifier() : "$anon") 
                      + "("+signature.toString()+")" 
                      + System.lineSeparator();

    r += " => Export? "+ toExport + System.lineSeparator();

    return r + body.repr();
  }

  public FunctionSignature getSignature() {
    return signature;
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  public boolean hasBoundName() {
    return boundName != null;
  }

  public Identifier getBoundName() {
    return boundName;
  }

  public Set<Identifier> getCaptures() {
    return captures;
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
