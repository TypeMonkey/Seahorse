package jg.sh.compile.pool.component;

import jg.sh.common.Location;
import jg.sh.compile.pool.ConstantPool.MutableIndex;

public class ErrorHandlingRecord extends PoolComponent {

  private final String startTryCatch;
  private final String endTryCatch;
  private final String catchLabel;
  
  public ErrorHandlingRecord(String startTryLabel, 
                             String endTryLabel, 
                             String catchLabel) {
    super(ComponentType.ERROR_RECORD);
    this.startTryCatch = startTryLabel;
    this.catchLabel = catchLabel;
    this.endTryCatch = endTryLabel;
  }
  
  public String getStartTryCatch() {
    return startTryCatch;
  }
  
  public String getEndTryCatch() {
    return endTryCatch;
  }
  
  public String getCatchLabel() {
    return catchLabel;
  }

  @Override
  public String toString() {
    return "<error_record> "+startTryCatch+" "+endTryCatch+" "+catchLabel;
  }
}
