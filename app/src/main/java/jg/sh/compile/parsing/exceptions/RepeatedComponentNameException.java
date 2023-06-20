package jg.sh.compile.parsing.exceptions;

public class RepeatedComponentNameException extends FormationException{

  public RepeatedComponentNameException(String compName, String fileName, int line, int column) {
    super(fileName, "There's already a component with the name '"+compName+"'", line, column);
  }

}
