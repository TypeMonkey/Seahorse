package jg.sh.parsing.nodes;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jg.sh.common.Location;
import jg.sh.parsing.Context;
import jg.sh.parsing.NodeVisitor;
import jg.sh.parsing.token.Token;

public class Operator extends Node {

  public static enum Op {
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

    private static Set<Op> mutatesLeft;

    private static final Map<String, Op> stringToOp = new HashMap<>();
    
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

  private final Op op; 

  public Operator(Token token) {
    super(token.getStart(), token.getEnd());
    this.op = Op.stringToOp.get(token.getContent());

    if (this.op == null) {
      throw new IllegalArgumentException("Unknown operator "+token);
    }
  }

  public Operator(Op op, Location start, Location end) {
    super(start, end);
    this.op = op;
  }

  public Op getOp() {
    return op;
  }

  @Override
  public <T, C extends Context<?>> T accept(NodeVisitor<T, C> visitor, C parentContext) {
    return visitor.visitOperator(parentContext, this);
  }

  @Override
  public String repr() {
    return op.str;
  }

  @Override
  public boolean isLValue() {
    return false;
  }
  
}
