package jg.sh.compile.parsing.nodes;

public enum ReservedWords {

  VAR("var"),
  CONST("const"),
  FUNC("func"),
  CONSTR("constr"),
  RETURN("return"),
  DATA("data"),
  
  FOR("for"),
  WHILE("while"),
  IF("if"),
  ELSE("else"),
  ELIF("elif"),
  SWITCH("switch"),
  DO("do"),
  DEFAULT("default"),
  CASE("case"),
  CONTINUE("continue"),
  BREAK("break"),
  TRY("try"),
  CATCH("catch"),  
  THROW("throw"),
  //NEW("new"),
  IS("is"),
    
  MODULE("module"),
  SELF("self"),
  CAPTURE("capture"),
  
  USE("use"),
  FROM("from"),
  
  EXPORT("export"),
    
  SEMICOLON(";"),
  OP_CURLY("{"),
  
  OP_SQ("[");

  
  public final String actualWord;
  
  private ReservedWords(String actualWord) {
    this.actualWord = actualWord;
  }
  
  /**
   * Returns the equivalent ReservedWord instance of the gievn keyword
   * @param keyWord - the keyword whose ReservedWord equivalent to return
   * @return the equivalent ReservedWord, or null if there's no such equivalent
   */
  public static ReservedWords stringToReservedWord(String keyWord) {
    try {
      if (keyWord.equals(SEMICOLON.actualWord)) {
        return SEMICOLON;
      }
      else if (keyWord.equals(OP_CURLY.actualWord)) {
        return OP_CURLY;
      }
      else if (keyWord.equals(OP_SQ.actualWord)) {
        return OP_SQ;
      }
      return ReservedWords.valueOf(keyWord.toUpperCase());
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
  
}
