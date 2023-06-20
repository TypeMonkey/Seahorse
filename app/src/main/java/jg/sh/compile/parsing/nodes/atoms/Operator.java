package jg.sh.compile.parsing.nodes.atoms;

import jg.sh.common.OperatorKind;
import jg.sh.compile.parsing.nodes.ASTNode;
import jg.sh.compile.parsing.nodes.NodeVisitor;

public class Operator extends ASTNode{
  
  private final OperatorKind op;
  
  public Operator(int line, int column, OperatorKind op) {
    super(line, column);
    this.op = op;
  }
  
  public OperatorKind getOp() {
    return op;
  }
  
  @Override
  public void acceptVisitor(NodeVisitor visitor) {
    visitor.visit(this);
  }
  
  @Override
  public boolean isLValue() {
    return false;
  }
  
  @Override
  public String toString() {
    return "OP ~ "+op;
  }
  
  public static OperatorKind stringToOp(String op) {
    switch (op) {
    case "+":
      return OperatorKind.PLUS;
    case "-":
      return OperatorKind.MINUS;
    case "*":
      return OperatorKind.TIMES;
    case "/":
      return OperatorKind.DIV;
    case "%":
      return OperatorKind.MOD;
    case "<":
      return OperatorKind.LESS;
    case ">":
      return OperatorKind.GREAT;
    case "==":
      return OperatorKind.EQUAL;
    case "<=":
      return OperatorKind.LESSQ;
    case ">=":
      return OperatorKind.GREATQ;
    case "!=":
      return OperatorKind.NOTEQUAL;
    case "=":
      return OperatorKind.ASSIGN;
    case "*=":
      return OperatorKind.MULT_EQ;
    case "+=":
      return OperatorKind.PLUS_EQ;
    case "&":
      return OperatorKind.BOOL_AND;
    case "|":
      return OperatorKind.BOOL_OR;
    case "/=":
      return OperatorKind.DIV_EQ;
    case "-=":
      return OperatorKind.MINUS_EQ;
    case "%=":
      return OperatorKind.MOD_EQ;
    case "&&":
      return OperatorKind.BOOL_AND;
    case "||":
      return OperatorKind.BOOL_OR;
    case "!":
      return OperatorKind.NOT;
    case "->":
      return OperatorKind.ARROW;
    case "is":
      return OperatorKind.IS;
    default:
      return null;
    }
  }
}
