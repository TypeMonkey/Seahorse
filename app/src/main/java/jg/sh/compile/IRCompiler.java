package jg.sh.compile;

import java.util.Collections;
import java.util.List;

import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompContext.IdentifierInfo;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.StringConstant;
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

import static jg.sh.compile.NodeResult.*;
import static jg.sh.compile.instrs.OpCode.*;

public class IRCompiler implements Visitor<NodeResult, CompContext> {

  public IRCompiler() {}

  @Override
  public NodeResult visitString(CompContext parentContext, Str str) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new StringConstant(str.getValue()));
    return valid(str, new ArgInstr(str.start, str.end, LOADC, index));
  }

  @Override
  public NodeResult visitInt(CompContext parentContext, Int integer) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new IntegerConstant(integer.getValue()));
    return valid(integer, new ArgInstr(integer.start, integer.end, LOADC, index));
  }

  @Override
  public NodeResult visitBoolean(CompContext parentContext, Bool bool) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new BoolConstant(bool.getValue()));
    return valid(bool, new ArgInstr(bool.start, bool.end, LOADC, index));
  }

  @Override
  public NodeResult visitFloat(CompContext parentContext, FloatingPoint floatingPoint) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new FloatConstant(floatingPoint.getValue()));
    return valid(floatingPoint, new ArgInstr(floatingPoint.start, floatingPoint.end, LOADC, index));
  }

  @Override
  public NodeResult visitNull(CompContext parentContext, Null nullVal) {
    return valid(nullVal, new NoArgInstr(nullVal.start, nullVal.end, LOADNULL));
  }

  @Override
  public NodeResult visitIdentifier(CompContext parentContext, Identifier identifier) {
    final IdentifierInfo identifierInfo = parentContext.getVariable(identifier.getIdentifier());

    if (identifierInfo == null) {
      return invalid(identifier, 
                     new ValidationException("Unfound identifier '"+identifier.getIdentifier()+"'.", 
                                             identifier.start, 
                                             identifier.end));
    }

    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      return valid(identifier, identifierInfo.getStoreInstr());
    }
    return valid(identifier, identifierInfo.getLoadInstr());
  }

  @Override
  public NodeResult visitKeyword(CompContext parentContext, Keyword keyword) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitKeyword'");
  }

  @Override
  public NodeResult visitParameter(CompContext parentContext, Parameter parameter) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitParameter'");
  }

  @Override
  public NodeResult visitFuncDef(CompContext parentContext, FuncDef funcDef) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitFuncDef'");
  }

  @Override
  public NodeResult visitOperator(CompContext parentContext, Operator operator) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitOperator'");
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitBinaryExpr'");
  }

  @Override
  public NodeResult visitParenthesized(CompContext parentContext, Parenthesized parenthesized) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitParenthesized'");
  }

  @Override
  public NodeResult visitObjectLiteral(CompContext parentContext, ObjectLiteral objectLiteral) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitObjectLiteral'");
  }

  @Override
  public NodeResult visitCall(CompContext parentContext, FuncCall funcCall) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitCall'");
  }

  @Override
  public NodeResult visitAttrAccess(CompContext parentContext, AttrAccess attrAccess) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitAttrAccess'");
  }

  @Override
  public NodeResult visitArray(CompContext parentContext, ArrayLiteral arrayLiteral) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitArray'");
  }

  @Override
  public NodeResult visitIndexAccess(CompContext parentContext, IndexAccess arrayAccess) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitIndexAccess'");
  }

  @Override
  public NodeResult visitVarDeclr(CompContext parentContext, VarDeclr varDeclr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitVarDeclr'");
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitUnary'");
  }

}
