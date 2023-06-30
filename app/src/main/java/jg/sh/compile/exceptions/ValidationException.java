package jg.sh.compile.exceptions;

import jg.sh.common.Location;

public class ValidationException extends Exception {
  
  public ValidationException(String message, int line, int column) {
    super(message+" at "+Location.toString(line, column));
  }

  public ValidationException(String message, Location singleLocation) {
    super(message+" at "+singleLocation);
  }

  public ValidationException(String message, Location start, Location end) {
    super(message+" at "+start+" <-> "+end);
  }

}
