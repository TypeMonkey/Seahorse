package jg.sh.irgen.pool.component;

public interface PoolComponent {
  
  public enum ComponentType{
    STRING,
    INT,
    FLOAT,
    BOOLEAN,
    ERROR_RECORD,
    CODE
  }
   
  public ComponentType getType();
  
  @Override
  public abstract String toString();

}
