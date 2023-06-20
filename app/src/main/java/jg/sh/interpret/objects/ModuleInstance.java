package jg.sh.interpret.objects;

public class ModuleInstance extends ClassInstance{
  
  private final String moduleName;  

  public ModuleInstance(String moduleName, int moduleCode) {
    super(moduleCode);
    this.moduleName = moduleName;
  }
  
  public String getModuleName() {
    return moduleName;
  }
  
  @Override
  public String toString() {
    return "<module> '"+moduleName+"'";
  }
  
  
}
