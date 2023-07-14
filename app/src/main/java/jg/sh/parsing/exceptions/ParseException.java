package jg.sh.parsing.exceptions;

import jg.sh.common.Location;

public class ParseException extends Exception {

  private final String module;

  public ParseException(String message, int line, int column, String module) {
    super(message+" at "+Location.toString(line, column));
    this.module = module;
  }

  public ParseException(String message, Location singleLocation, String module) {
    super(message+" at "+singleLocation);
    this.module = module;
  }

  public ParseException(String message, Location start, Location end, String module) {
    super(message+" at "+start+" <-> "+end);
    this.module = module;
  }

  public String getModule() {
    return module;
  }
}
