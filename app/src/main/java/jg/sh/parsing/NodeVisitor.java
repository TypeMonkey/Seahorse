package jg.sh.parsing;

import jg.sh.parsing.nodes.IndexAccess;
import jg.sh.parsing.nodes.ArrayLiteral;
import jg.sh.parsing.nodes.AttrAccess;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.ConstAttrDeclr;
import jg.sh.parsing.nodes.FuncCall;
import jg.sh.parsing.nodes.FuncDef;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Keyword;
import jg.sh.parsing.nodes.ObjectLiteral;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.statements.CaptureStatement;
import jg.sh.parsing.nodes.statements.DataDefinition;
import jg.sh.parsing.nodes.statements.ReturnStatement;
import jg.sh.parsing.nodes.statements.Statement;
import jg.sh.parsing.nodes.statements.UseStatement;
import jg.sh.parsing.nodes.statements.blocks.Block;
import jg.sh.parsing.nodes.statements.blocks.IfBlock;
import jg.sh.parsing.nodes.statements.blocks.TryCatch;
import jg.sh.parsing.nodes.statements.blocks.WhileBlock;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;

public interface NodeVisitor<T, C extends Context<?>> {

  public T visitString(C parentContext, Str str);

  public T visitInt(C parentContext, Int integer);

  public T visitBoolean(C parentContext, Bool bool);

  public T visitFloat(C parentContext, FloatingPoint floatingPoint);

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

  public T visitUnary(C parentContext, UnaryExpr unaryExpr);

  public T visitStatement(C parentContext, Statement statement);

  public T visitUseStatement(C parentContext, UseStatement useStatement);

  public T visitReturnStatement(C parentContext, ReturnStatement returnStatement);

  public T visitDataDefinition(C parentContext, DataDefinition dataDefinition);

  public T visitCaptureStatement(C parentContext, CaptureStatement captureStatement);

  public T visitBlock(C parentContext, Block block);

  public T visitIfBlock(C parentContext, IfBlock ifBlock);

  public T visitTryCatchBlock(C parentContext, TryCatch tryCatch);

  public T visitWhileBlock(C parentContext, WhileBlock whileBlock);

  public T visitConstAttrDeclr(C parentContext, ConstAttrDeclr constAttrDeclr);
  
}
