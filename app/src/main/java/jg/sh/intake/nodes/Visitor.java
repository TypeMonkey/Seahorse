package jg.sh.intake.nodes;

import jg.sh.intake.nodes.constructs.FuncDef;
import jg.sh.intake.nodes.constructs.Statement;
import jg.sh.intake.nodes.constructs.UseDeclaration;
import jg.sh.intake.nodes.constructs.VariableDeclr;
import jg.sh.intake.nodes.constructs.blocks.BlockExpr;
import jg.sh.intake.nodes.constructs.blocks.IfBlock;
import jg.sh.intake.nodes.constructs.blocks.WhileBlock;
import jg.sh.intake.nodes.simple.ArrayLiteral;
import jg.sh.intake.nodes.simple.AttrAccess;
import jg.sh.intake.nodes.simple.BinaryOpExpr;
import jg.sh.intake.nodes.simple.FuncCallExpr;
import jg.sh.intake.nodes.simple.Identifier;
import jg.sh.intake.nodes.simple.IndexAccessExpr;
import jg.sh.intake.nodes.simple.Keyword;
import jg.sh.intake.nodes.simple.ObjectLiteralExpr;
import jg.sh.intake.nodes.simple.Operator;
import jg.sh.intake.nodes.simple.Parameter;
import jg.sh.intake.nodes.simple.ParenthesizedExpr;
import jg.sh.intake.nodes.simple.TernaryExpr;
import jg.sh.intake.nodes.simple.UnaryExpr;
import jg.sh.intake.nodes.values.Bool;
import jg.sh.intake.nodes.values.Int;
import jg.sh.intake.nodes.values.NullValue;
import jg.sh.intake.nodes.values.Str;
import jg.sh.intake.nodes.values.Float;

public interface Visitor<T, C extends Context> {

    public T visitInt(C parentContext, Int node);

    public T visitFloat(C parentContext , Float node);

    public T visitStr(C parentContext , Str node);

    public T visitOperator(C parentContext , Operator node);

    public T visitBinary(C parentContext , BinaryOpExpr node);

    public T visitKeyword(C parentContext , Keyword node);

    public T visitTernary(C parentContext , TernaryExpr node);

    public T visitUnary(C parentContext , UnaryExpr node);

    public T visitCall(C parentContext , FuncCallExpr node);

    public T visitIndexAccess(C parentContext , IndexAccessExpr node);

    public T visitIdentifier(C parentContext , Identifier node);

    public T visitStatement(C parentContext , Statement node);

    public T visitBlock(C parentContext , BlockExpr node);

    public T visitNull(C parentContext , NullValue node);

    public T visitVarDec(C parentContext , VariableDeclr node);

    public T visitIfBlock(C parentContext , IfBlock node);

    public T visitWhileBlock(C parentContext , WhileBlock node);

    public T visitArrayLiteral(C parentContext , ArrayLiteral node);

    public T visitParenthesized(C parentContext , ParenthesizedExpr node);

    public T visitBool(C parentContext , Bool node);

    public T visitFuncDef(C parentContext , FuncDef node);

    public T visitAttrAccess(C parentContext , AttrAccess node);

    public T visitUseDeclaration(C parentContext, UseDeclaration useDeclaration);

    public T visitFunc(C parentContext, FuncDef funcDef);

    public T visitParameter(C parentContext, Parameter parameter);

    public T visitObjectLiteral(C parentContext, ObjectLiteralExpr objectLiteralExpr);
}
