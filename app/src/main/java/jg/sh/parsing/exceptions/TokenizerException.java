package jg.sh.parsing.exceptions;

import jg.sh.common.Location;

public class TokenizerException extends Exception {
  
  public TokenizerException(String message, int line, int column){
    super(message+" at "+Location.toString(line, column));
  }

}
