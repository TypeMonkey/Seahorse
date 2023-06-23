package jg.sh.intake.nodes.constructs;

import java.util.Map;

import jg.sh.intake.Location;
import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Visitor;
import jg.sh.intake.nodes.simple.Identifier;

/**
 * A use declaration is a top-level only statement that 
 * intakes another module (or its functions, data definitions and variables) for use inside a module.
 * 
 * Use declarations are of the following format:
 *      use <module name> ;
 * or
 *      use <module name>::<module component name> (, <module component name>)* ;
 * 
 * Example: 
 *      use ModuleOne;  //Imports the module named "ModuleOne"
 *      use ModuleOne::funky; //Imports the module component from "ModuleOne" called "funky", but doesn't import ModuleOne
 *      use ModuleOne::funky, boo, bar; //Imports the funky, boo, and bar components from ModuleOne
 * 
 * Alias - Use declaration can append an alias as to not conflict with any equally-named component
 *         in the module:
 *  
 *      use ModuleOne as Mod; //ModuleOne now has the alias of "Mod" in the module
 *      use ModuleOne::funky as func; //funky can be now be used as "func"
 *      use ModuleOne::funky as fy, boo as apple; //funky can be used as "fy" and boo as "apple"
 */
public class UseDeclaration extends Statement {
  
  private final String moduleName;
  private final Map<Identifier, Identifier> componentNames;
  private final Identifier alias;

  public UseDeclaration(Location start, 
                        Location end, 
                        String moduleName, 
                        Map<Identifier, Identifier> componentNames, 
                        Identifier alias) {
    super(start, end);
    this.moduleName = moduleName;
    this.componentNames = componentNames;
    this.alias = alias;
  }

  public String getModuleName() {
    return moduleName;
  }

  public Map<Identifier, Identifier> getComponentNames() {
    return componentNames;
  }

  public Identifier getAlias() {
    return alias;
  }

  @Override
  public String toString() {
    return "use "+moduleName
                 +( componentNames != null ? "::"+componentNames : "")
                 +( alias != null ? " -> " + alias.getName() : "");
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext) {
    return null;
  }

  @Override
  public String repr() {
    return toString();
  }

  @Override
  public boolean isLValue() {
    return false;
  }
}
