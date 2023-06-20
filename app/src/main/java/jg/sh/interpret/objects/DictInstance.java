package jg.sh.interpret.objects;

import jg.sh.interpret.TypeConstants;

public class DictInstance extends ClassInstance{
  
  public DictInstance() {
    super(TypeConstants.DICT_TYPE_CODE);  
  }

  @Override
  public String toString() {
    return "<dict> "+getAttrs();
  }

}
