package jg.sh.compile_old.parsing.nodes;

import jg.sh.compile_old.parsing.nodes.atoms.ArrayAccess;
import jg.sh.compile_old.parsing.nodes.atoms.ArrayLiteral;
import jg.sh.compile_old.parsing.nodes.atoms.AttrAccess;
import jg.sh.compile_old.parsing.nodes.atoms.FuncDef;
import jg.sh.compile_old.parsing.nodes.atoms.Identifier;
import jg.sh.compile_old.parsing.nodes.atoms.Keyword;
import jg.sh.compile_old.parsing.nodes.atoms.NullValue;
import jg.sh.compile_old.parsing.nodes.atoms.ObjectLiteral;
import jg.sh.compile_old.parsing.nodes.atoms.Operator;
import jg.sh.compile_old.parsing.nodes.atoms.Parameter;
import jg.sh.compile_old.parsing.nodes.atoms.Parenthesized;
import jg.sh.compile_old.parsing.nodes.atoms.Unary;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Bool;
import jg.sh.compile_old.parsing.nodes.atoms.constants.FloatingPoint;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Int;
import jg.sh.compile_old.parsing.nodes.atoms.constants.Str;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.Module;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.IfElse;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.ScopeBlock;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.TryCatch;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.blocks.WhileLoop;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.CaptureStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.ExpressionStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.KeywordStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.UseStatement;
import jg.sh.compile_old.parsing.nodes.atoms.constructs.statements.VariableStatement;

public interface NodeVisitor {

  public void visit(BinaryOpExpr expr);
  
  public void visit(CallArg expr);

  public void visit(FunctionCall expr);
  
  
  public void visit(ArrayAccess expr);
  
  public void visit(ArrayLiteral expr);

  public void visit(AttrAccess expr);
  
  public void visit(Bool expr);

  public void visit(ObjectLiteral expr);

  public void visit(FloatingPoint expr);

  public void visit(Identifier expr);

  public void visit(Int expr);

  public void visit(Keyword expr);

  public void visit(NullValue expr);

  public void visit(Operator expr);
  
  public void visit(Parameter expr);
  
  public void visit(Parenthesized expr);

  public void visit(Str expr);
  
  public void visit(Unary expr);
  

  public void visit(FuncDef expr);
  
  public void visit(UseStatement expr);
  
  public void visit(VariableStatement expr);
  
  public void visit(ExpressionStatement expr);

  public void visit(KeywordStatement expr);
  
  public void visit(CaptureStatement statement);
  
  public void visit(Module expr);
  
  
  public void visit(ScopeBlock expr);

  public void visit(IfElse expr);

  public void visit(TryCatch expr);

  public void visit(WhileLoop expr);
}
