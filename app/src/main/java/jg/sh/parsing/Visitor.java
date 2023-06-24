package jg.sh.parsing;

import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.ArrayLiteral;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.ObjectLiteral;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.Float;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;

public interface Visitor<T, C extends Context> {

  public T visitString(C parentContext, Str str);

  public T visitInt(C parentContext, Int integer);

  public T visitBoolean(C parentContext, Bool bool);

  public T visitFloat(C parentContext, Float floatingPoint);

  public T visitNull(C parentContext, Null nullVal);

  public T visitIdentifier(C parentContext, Identifier identifier);

  public T visitKeyword(C parentContext, Keyword keyword);

  public T visitParameter(C parentContext, Parameter parameter);

  public T visitFuncDef(C parentContext, FuncDef funcDef);

  public T visitOperator(C parentContext, Operator operator);

  public T visitBinaryExpr(C parentContext, BinaryOpExpr binaryOpExpr);

  public T visitParenthesized(C parentContext, Parenthesized parenthesized);

  public T visitObjectLiteral(C parentContext, ObjectLiteral objectLiteral);

  public T visitCall(C parentContext, FuncCall funcCall);

  public T visitAttrAccess(C parentContext, AttrAccess attrAccess);

  public T visitArray(C parentContext, ArrayLiteral arrayLiteral);

  public T visitIndexAccess(C parentContext, IndexAccess arrayAccess);

  public T visitVarDeclr(C parentContext, VarDeclr varDeclr);
  
}
