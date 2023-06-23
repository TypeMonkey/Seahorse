package jg.sh.intake.nodes.simple;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import jg.sh.intake.nodes.Context;
import jg.sh.intake.nodes.Node;
import jg.sh.intake.Location;
import jg.sh.intake.nodes.Visitor;

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
    }

    public final String str;

    private Op(String str) {
        this.str = str;
    }

    public static boolean mutatesLeft(Op op) {
        return mutatesLeft.contains(op);
    }
  }

  private final Op op;

  public Operator(Op op, Location start, Location end) {
    super(start, end);
    this.op = op;
  }

  @Override
  public <T, C extends Context> T accept(Visitor<T, C> visitor, C parentContext){
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
  
  public Op getOp() {
    return op;
  }
}
