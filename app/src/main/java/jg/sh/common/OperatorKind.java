package jg.sh.common;

public enum OperatorKind{
  PLUS,
  MINUS,
  TIMES,
  DIV,
  MOD,
  LESS, 
  GREAT, 
  LESSQ,
  GREATQ,
  EQUAL,
  NOTEQUAL,
  ARROW,
  IS,
  
  ASSIGN,
  MULT_EQ,
  PLUS_EQ,
  DIV_EQ,
  MINUS_EQ,
  MOD_EQ,
  
  BIT_AND,
  BIT_OR,
  
  BOOL_AND,
  BOOL_OR,
  
  NOT;
  
  public static boolean operatorMutatesLeft(OperatorKind op) {
    return op == ASSIGN || 
           op == MULT_EQ || 
           op == PLUS_EQ || 
           op == DIV_EQ || 
           op == MINUS_EQ || 
           op == MOD_EQ;
  }
  
  public static OperatorKind getMutatorOperator(OperatorKind op) {
    switch(op) {
    case ASSIGN: return ASSIGN;
    case MULT_EQ: return TIMES;
    case PLUS_EQ: return PLUS;
    case DIV_EQ: return DIV;
    case MINUS_EQ: return MINUS;
    case MOD_EQ: return MOD;
    default: return null;
    }
  }
  
}
