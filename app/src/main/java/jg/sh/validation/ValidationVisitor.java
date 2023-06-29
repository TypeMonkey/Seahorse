package jg.sh.validation;

import java.util.List;

import jg.sh.parsing.Visitor;
import jg.sh.parsing.nodes.ArrayLiteral;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.ObjectLiteral;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;

public class ValidationVisitor implements Visitor<List<ValidationException>, ValidationContext> {

  @Override
  public List<ValidationException> visitString(ValidationContext parentContext, Str str) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitString'");
  }

  @Override
  public List<ValidationException> visitInt(ValidationContext parentContext, Int integer) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitInt'");
  }

  @Override
  public List<ValidationException> visitBoolean(ValidationContext parentContext, Bool bool) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitBoolean'");
  }

  @Override
  public List<ValidationException> visitFloat(ValidationContext parentContext, FloatingPoint floatingPoint) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitFloat'");
  }

  @Override
  public List<ValidationException> visitNull(ValidationContext parentContext, Null nullVal) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitNull'");
  }

  @Override
  public List<ValidationException> visitIdentifier(ValidationContext parentContext, Identifier identifier) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitIdentifier'");
  }

  @Override
  public List<ValidationException> visitKeyword(ValidationContext parentContext, Keyword keyword) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitKeyword'");
  }

  @Override
  public List<ValidationException> visitParameter(ValidationContext parentContext, Parameter parameter) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitParameter'");
  }

  @Override
  public List<ValidationException> visitFuncDef(ValidationContext parentContext, FuncDef funcDef) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitFuncDef'");
  }

  @Override
  public List<ValidationException> visitOperator(ValidationContext parentContext, Operator operator) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
  }

  @Override
  public List<ValidationException> visitBinaryExpr(ValidationContext parentContext, BinaryOpExpr binaryOpExpr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
  }

  @Override
  public List<ValidationException> visitParenthesized(ValidationContext parentContext, Parenthesized parenthesized) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitParenthesized'");
  }

  @Override
  public List<ValidationException> visitObjectLiteral(ValidationContext parentContext, ObjectLiteral objectLiteral) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitObjectLiteral'");
  }

  @Override
  public List<ValidationException> visitCall(ValidationContext parentContext, FuncCall funcCall) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
  }

  @Override
  public List<ValidationException> visitAttrAccess(ValidationContext parentContext, AttrAccess attrAccess) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitAttrAccess'");
  }

  @Override
  public List<ValidationException> visitArray(ValidationContext parentContext, ArrayLiteral arrayLiteral) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitArray'");
  }

  @Override
  public List<ValidationException> visitIndexAccess(ValidationContext parentContext, IndexAccess arrayAccess) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitIndexAccess'");
  }

  @Override
  public List<ValidationException> visitVarDeclr(ValidationContext parentContext, VarDeclr varDeclr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitVarDeclr'");
  }

  @Override
  public List<ValidationException> visitUnary(ValidationContext parentContext, UnaryExpr unaryExpr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitUnary'");
  }

}
