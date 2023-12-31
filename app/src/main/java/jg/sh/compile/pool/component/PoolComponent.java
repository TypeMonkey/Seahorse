package jg.sh.compile.pool.component;

public interface PoolComponent {
  
  public enum ComponentType{
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    ERROR_RECORD,
    DATA_RECORD,
    CODE
  }
   
  public ComponentType getType();
  
  @Override
  public abstract String toString();

}
