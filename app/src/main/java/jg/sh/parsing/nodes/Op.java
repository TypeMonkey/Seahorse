package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public enum Op {
  NOT_EQ("!="),
  GR_EQ(">="), 
  LS_EQ("<="),
  PLUS("+"),
  MINUS("-"),
  MULT("*"),
  LESS("<"),
  GREAT(">"),
  EQUAL("="),
  EXPONENT("^"),
  ASSIGNMENT(":="),
  ARROW("->"),
  DIV("/"),
  MOD("%"),
  AND("&"),
  OR("|"),
  EQ_MULT("*="),
  EQ_ADD("+="),
  EQ_DIV("/="),
  EQ_MIN("-="),
  EQ_MOD("%="),
  EQ_EXPO("^="),
  BOOL_AND("&&"),
  BOOL_OR("||"),

  //Unary
  BANG("!");

  public static enum PrecedenceLevel {
    ASSIGN(false, ASSIGNMENT, EQ_MULT, EQ_ADD, EQ_DIV, EQ_MIN, EQ_MOD, EQ_EXPO),
    BOOL_OR(true, Op.BOOL_OR),
    BOOL_AND(true, Op.BOOL_AND),
    BIT_OR(true, OR),
    BIT_AND(true, AND),
    EQUALITY(true, EQUAL, NOT_EQ),
    COMPARISON(true, LESS, GREAT, GR_EQ, LS_EQ),
    PLUS_MIN(true, PLUS, MINUS),
    DIV_MUL(true, DIV, MULT, MOD, EXPONENT),
    UNARY(false, BANG, MINUS);

    public final Set<Op> opsAtLevel;
    public final boolean isLeftRight;

    private PrecedenceLevel(boolean isLeftRight, Op ... ops){
      this.isLeftRight = isLeftRight;
      this.opsAtLevel = new HashSet<>(Arrays.asList(ops));
    }

    public static PrecedenceLevel getOpPrecedenceLevel(Op op) {
      for (PrecedenceLevel level : PrecedenceLevel.values()) {
        if (level.opsAtLevel.contains(op)) {
          return level;
        }
      }
      return null;
    }

    public static boolean isOpPrecedenceLevelLeftRight(Op op) {
      final PrecedenceLevel level = getOpPrecedenceLevel(op);
      if (level != null) {
        return level.isLeftRight;
      }
      throw new IllegalArgumentException("Unfound precedence level for "+op.name()+": '"+op.str+"'");
    }
  }

  private static final Set<Op> mutatesLeft;

  protected static final Map<String, Op> stringToOp;
  
  static {
    mutatesLeft = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
      ASSIGNMENT,
      EQ_MULT,
      EQ_ADD,
      EQ_DIV,
      EQ_MIN,
      EQ_MOD,
      EQ_EXPO
    )));

    stringToOp = new HashMap<>();
    for (Op op : Op.values()) {
      stringToOp.put(op.str, op);
    }
  }

  public final String str;

  private Op(String str) {
    this.str = str;
  }

  public static boolean mutatesLeft(Op op) {
    return mutatesLeft.contains(op);
  }

  public static Op getMutatorOperator(Op op) {
    switch(op) {
      case ASSIGNMENT: return ASSIGNMENT;
      case EQ_MULT: return MULT;
      case EQ_ADD: return PLUS;
      case EQ_DIV: return DIV;
      case EQ_MIN: return MINUS;
      case EQ_MOD: return MOD;
      default: return null;
    }
  }
}
