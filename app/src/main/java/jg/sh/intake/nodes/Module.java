package jg.sh.intake.nodes;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jg.sh.intake.nodes.constructs.FuncDef;
import jg.sh.intake.nodes.constructs.Statement;
import jg.sh.intake.nodes.constructs.UseDeclaration;
import jg.sh.intake.nodes.constructs.VariableDeclr;

/**
 * A Turtle module is a collection of uniquely identifiable (by name) top-level
 * functions, data definitions and variables.
 * 
 * A Module should be uniquely identifiable by its full idenitifer, with respect
 * to a designated root directory which contains it.
 * 
 * For example, say we have the "Main.turtle" module located within in the
 * following
 * directory structure:
 * -> tr/
 *    -> sub1/
 *       -> Utils.tr
 *    -> sub2/
 *       -> Main.tr
 * 
 * The full identifier of "Main.tr" is "tr.sub2.Main"
 * 
 * Say "Main.tr" has a top-level function called "funky".
 * The full identifier of "funky" is: "tr.sub2.Main::funky"
 */
public class Module {

  private final String fullIdentifier;
  private final Map<String, FuncDef> functions;
  private final Map<String, VariableDeclr> vars;
  private final List<Statement> statements;

  public Module(String fullIdentifier,
      Map<String, FuncDef> functions,
      Map<String, VariableDeclr> vars,
      List<Statement> statements) {
    this.fullIdentifier = fullIdentifier;
    this.functions = functions;
    this.vars = vars;
    this.statements = statements;
  }

  public boolean isSymbolPresent(String name) {
    return functions.containsKey(name) || vars.containsKey(name);
  }

  public Map<String, FuncDef> getFunctions() {
    return functions;
  }

  public Map<String, VariableDeclr> getVars() {
    return vars;
  }

  public List<Statement> getUseDeclarations() {
    return statements;
  }

  public String getName() {
    final String [] nameSplit = fullIdentifier.split("\\.");
    System.out.println(Arrays.toString(nameSplit));
    return nameSplit[nameSplit.length - 1];
  }

  public String getFullIdentifier() {
    return fullIdentifier;
  }

  @Override
  public String toString() {
    String r = "  *** Module: " + fullIdentifier;
    r += functions.values().stream().map(Node::repr).collect(Collectors.joining(System.lineSeparator()));
    r += vars.values().stream().map(Node::repr).collect(Collectors.joining(System.lineSeparator()));
    // r +=
    // interfaces.values().stream().map(Node::repr).collect(Collectors.joining(System.lineSeparator()));
    r += statements.stream().map(Node::repr).collect(Collectors.joining(System.lineSeparator()));
    return r;
  }
}
