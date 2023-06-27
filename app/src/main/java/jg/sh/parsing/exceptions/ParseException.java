package jg.sh.parsing.exceptions;

import jg.sh.common.Location;

public class ParseException extends Exception {

  public ParseException(String message, int line, int column) {
    super(message+" at "+Location.toString(line, column));
  }

  public ParseException(String message, Location singleLocation) {
    super(message+" at "+singleLocation);
  }

  public ParseException(String message, Location start, Location end) {
    super(message+" at "+start+" <-> "+end);
  }
}
