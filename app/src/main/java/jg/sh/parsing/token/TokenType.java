package jg.sh.parsing.token;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public enum TokenType {
  IDENTIFIER,

  //literals
  STRING,
  INTEGER,
  DECIMAL,
  BOOLEAN,

  //KEYWORDS - START

  //value literals
  TRUE,
  FALSE,
  NULL,

  //Declarative keywords
  VAR,//
  FUNC,//
  CONSTR,//
  DATA,//
  OBJECT,
  SEALED,
  USE,//
  CONST,//
  SELF,//
  MODULE,
  BREAK,//
  CONTINUE,//
  CAPTURE,
  RETURN,//
  IS,//
  THROW,//
  EXPORT,//
  AS, //

  //control flow keywords
  WHILE,//
  FOR,//
  IF,//
  ELIF,//
  TRY,//
  CATCH,//
  ELSE,//

  //KEYWORDS - END

  //Symbols
  LEFT_PAREN,
  RIGHT_PAREN,
  LEFT_SQ_BR,
  RIGHT_SQ_BR,
  LEFT_CURL,
  RIGHT_CURL,
  QUESTION,
  COLON,
  COMMA,
  DOT,
  SEMICOLON,

  //binary operators
  NOT_EQ,
  GR_EQ, 
  ARROW,
  LS_EQ,
  PLUS,
  MINUS,
  MULT,
  LESS,
  GREAT,
  EQUAL,  //checks if two objects are equal (reference and value)
  EXPONENT,
  ASSIGNMENT,
  DIV,
  MOD,
  AND,
  OR,
  EQ_MULT,
  EQ_ADD,
  EQ_DIV,
  EQ_MIN,
  EQ_MOD,
  EQ_EXPO,
  BOOL_AND,
  BOOL_OR,

  //Unary
  BANG,
  
  //meta (comments,annotations, etc.)
  COMMENT_BLOCK_START,  // "/*"
  COMMENT_BLOCK_END,     // "*/"

  //End of file
  EOF
  ;

  public static final Set<String> keywords;
  public static final Set<TokenType> binOps;

  static {
    final HashSet<String> keyWordTemp = new HashSet<>();

    for(int i = TRUE.ordinal(); i <= ELSE.ordinal(); i++){
      keyWordTemp.add(TokenType.values()[i].name());
    }

    keywords = Collections.unmodifiableSet(keyWordTemp);

    final HashSet<TokenType> binOpsTemp = new HashSet<>();

    for(int i = NOT_EQ.ordinal(); i <= BOOL_OR.ordinal(); i++){
      binOpsTemp.add(TokenType.values()[i]);
    }

    binOps = Collections.unmodifiableSet(binOpsTemp);
  }

  public static boolean isKeyword(String potential){
    return keywords.contains(potential.toUpperCase());
  }

  public static boolean isKeyword(TokenType type){
    return type.ordinal() >= TRUE.ordinal() && type.ordinal() <= ELSE.ordinal();
  }

  public static boolean isBinOp(TokenType type){
    return type.ordinal() >= NOT_EQ.ordinal() && type.ordinal() <= BOOL_OR.ordinal();
  }
}
