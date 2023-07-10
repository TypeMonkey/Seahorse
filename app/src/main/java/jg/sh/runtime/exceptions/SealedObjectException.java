package jg.sh.runtime.exceptions;

public class SealedObjectException extends RuntimeException {
  
  public SealedObjectException(String mutatedAttr) {
    super("Can't mutate or append '"+mutatedAttr+"' as object is sealed.");
  }

}
