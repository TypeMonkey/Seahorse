package jg.sh.intake.nodes.constructs;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.VariableStatement;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.constructs.blocks.BlockExpr;
import jg.sh.intake.nodes.simple.Parameter;

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
 */
public class FuncDef extends Node {

  public static final String DATA_CONSTR_NAME_ATTR = "type";
  
  private final String boundName;
  private final FunctionSignature signature;
  private final CaptureStatement captures;
  private final LinkedHashMap<String, Parameter> allParams;
  private final boolean export;
  private final BlockExpr body;
    
  public FuncDef(Location start, 
                 Location end, 
                 String boundName,
                 FunctionSignature signature, 
                 CaptureStatement captures,
                 LinkedHashMap<String, Parameter> allParams,
                 boolean export,
                 BlockExpr body) {
    super(start, end);
    this.boundName = boundName;
    this.body = body;
    this.captures = captures;
    this.allParams = allParams;
    this.export = export;
    this.signature = signature;
  }

  public boolean isExportable() {
    return export;
  }
  
  public BlockExpr getBody() {
    return body;
  }
  
  public FunctionSignature getSignature() {
    return signature;
  }
  
  public LinkedHashMap<String, Parameter> getParams() {
    return allParams;
  }
  
  public CaptureStatement getCaptures() {
    return captures;
  }
  
  public String getBoundName() {
    return boundName;
  }
  
  public boolean hasName() {
    return boundName != null;
  }

  @Override
  public boolean isLValue() {
    return false;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return visitor.visitFunc(parentContext, this);
  }

  @Override
  public String repr() {
    String x = "~FUNC "+getBoundName()+System.lineSeparator();
    x += "  * SIGNATURE: "+ allParams + System.lineSeparator();
    x += "     "+getBody().getStatements()
                   .stream()
                   .map(s -> s.toString())
                   .collect(Collectors.joining(System.lineSeparator()));
    return x;
  }
}
