package jg.sh.interpret.objects;

import java.util.LinkedHashMap;
import java.util.Map;

public class ClassInstance extends Instance{
  
  private final Map<String, Integer> attrs;
  
  public ClassInstance(int typeCode) {
    super(typeCode);
    attrs = new LinkedHashMap<>();
  }
  
  public Integer setAttr(String attrName, Integer value) {
    return attrs.put(attrName, value);
  }
  
  public boolean attrExists(String name) {
    return attrs.containsKey(name);
  }
  
  public Integer getAttr(String name) {
    return attrs.get(name);
  }
  
  public Map<String, Integer> getAttrs() {
    return attrs;
  }
  
  @Override
  public String toString() {
    return "<type: "+getTypeCode()+">: "+attrs;
  }
}
