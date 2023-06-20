package jg.sh.compile.parsing.nodes;

import jg.sh.compile.parsing.nodes.atoms.ArrayAccess;
import jg.sh.compile.parsing.nodes.atoms.ArrayLiteral;
import jg.sh.compile.parsing.nodes.atoms.AttrAccess;
import jg.sh.compile.parsing.nodes.atoms.ObjectLiteral;
import jg.sh.compile.parsing.nodes.atoms.FuncDef;
import jg.sh.compile.parsing.nodes.atoms.Identifier;
import jg.sh.compile.parsing.nodes.atoms.Keyword;
import jg.sh.compile.parsing.nodes.atoms.NullValue;
import jg.sh.compile.parsing.nodes.atoms.Operator;
import jg.sh.compile.parsing.nodes.atoms.Parameter;
import jg.sh.compile.parsing.nodes.atoms.Parenthesized;
import jg.sh.compile.parsing.nodes.atoms.Unary;
import jg.sh.compile.parsing.nodes.atoms.constants.Bool;
import jg.sh.compile.parsing.nodes.atoms.constants.FloatingPoint;
import jg.sh.compile.parsing.nodes.atoms.constants.Int;
import jg.sh.compile.parsing.nodes.atoms.constants.Str;
import jg.sh.compile.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.IfElse;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.ScopeBlock;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.TryCatch;
import jg.sh.compile.parsing.nodes.atoms.constructs.blocks.WhileLoop;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.CaptureStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.ExpressionStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.KeywordStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.UseStatement;
import jg.sh.compile.parsing.nodes.atoms.constructs.statements.VariableStatement;

public class NodeVisitorAdapter implements NodeVisitor {

  @Override
  public void visit(BinaryOpExpr expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(CallArg expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(FunctionCall expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(ArrayAccess expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(ArrayLiteral expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(AttrAccess expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Bool expr) {
    // TODO Auto-generated method stub
  }
  
  @Override
  public void visit(ObjectLiteral expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(FloatingPoint expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Identifier expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Int expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Keyword expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(NullValue expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Operator expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Parameter expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Parenthesized expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Str expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Unary expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(FuncDef expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(UseStatement expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(VariableStatement expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(Module expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(IfElse expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(TryCatch expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(WhileLoop expr) {
    // TODO Auto-generated method stub
  }

  @Override
  public void visit(ExpressionStatement expr) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visit(KeywordStatement expr) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visit(ScopeBlock expr) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void visit(CaptureStatement statement) {
    // TODO Auto-generated method stub
    
  }

}
