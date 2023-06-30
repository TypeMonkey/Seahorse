package jg.sh.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompContext.ContextType;
import jg.sh.compile.CompContext.IdentifierInfo;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.StoreCellInstr;
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
import jg.sh.parsing.nodes.Node;
import jg.sh.parsing.nodes.ObjectLiteral;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Parameter;
import jg.sh.parsing.nodes.Parenthesized;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.VarDeclr;
import jg.sh.parsing.nodes.FuncCall.Argument;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Null;
import jg.sh.parsing.nodes.values.Str;
import jg.sh.parsing.token.TokenType;

import static jg.sh.compile.NodeResult.*;
import static jg.sh.compile.instrs.OpCode.*;

public class IRCompiler implements Visitor<NodeResult, CompContext> {

  public IRCompiler() {}

  @Override
  public NodeResult visitString(CompContext parentContext, Str str) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new StringConstant(str.getValue()));
    return valid(new ArgInstr(str.start, str.end, LOADC, index));
  }

  @Override
  public NodeResult visitInt(CompContext parentContext, Int integer) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new IntegerConstant(integer.getValue()));
    return valid(new ArgInstr(integer.start, integer.end, LOADC, index));
  }

  @Override
  public NodeResult visitBoolean(CompContext parentContext, Bool bool) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new BoolConstant(bool.getValue()));
    return valid(new ArgInstr(bool.start, bool.end, LOADC, index));
  }

  @Override
  public NodeResult visitFloat(CompContext parentContext, FloatingPoint floatingPoint) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    int index = constantPool.addComponent(new FloatConstant(floatingPoint.getValue()));
    return valid(new ArgInstr(floatingPoint.start, floatingPoint.end, LOADC, index));
  }

  @Override
  public NodeResult visitNull(CompContext parentContext, Null nullVal) {
    return valid(new NoArgInstr(nullVal.start, nullVal.end, LOADNULL));
  }

  @Override
  public NodeResult visitIdentifier(CompContext parentContext, Identifier identifier) {
    final IdentifierInfo identifierInfo = parentContext.getVariable(identifier.getIdentifier());

    if (identifierInfo == null) {
      return invalid(new ValidationException("'"+identifier.getIdentifier()+"' is unfound.", 
                                             identifier.start, 
                                             identifier.end));
    }

    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      return valid(identifierInfo.getStoreInstr());
    }
    return valid(identifierInfo.getLoadInstr());
  }

  @Override
  public NodeResult visitKeyword(CompContext parentContext, Keyword keyword) {
    // return; and return <expr>; are always wrapped as a ReturnStatement
    switch (keyword.getKeyword()) {
      case BREAK: 
      case CONTINUE: {
        final CompContext loopContext = parentContext.getNearestContext(ContextType.LOOP);
        final CompContext funcContext = parentContext.getNearestContext(ContextType.FUNCTION);

        if (loopContext == null || 
            funcContext.getNearestContext(ContextType.LOOP) == loopContext) {
          /*
           * First check: If the break/continue isn't in any loop at all, then
           *              it's invalid
           *
           * The second check works like this:
           * 
           * We get the contexts corresponding to the nearest loop and function. A break and continue
           * must exist within a loop context, so if there's none found, that's a trivial error.
           * 
           * But to check for the more difficult condition: when our break and continue is within a function
           * that's inside a loop, we get the context corresponding to the nearest function
           * 
           * We then check the context of the corresponding function - if there is one - if it's within a loop and getting
           * the corresponding context object of it. We then check - by plain reference equality - if this loop context object
           * is the same as the loop context object we had. 
           */
          return invalid(new ValidationException(keyword.repr()+" isn't within a loop.", keyword.start, keyword.end));
        }

        final String targetLabel = (String) parentContext.getValue(keyword.getKeyword() == TokenType.BREAK ? 
                                                                      ContextKey.BREAK_LOOP_LABEL : 
                                                                      ContextKey.CONT_LOOP_LABEL);
        return valid(new JumpInstr(keyword.start, keyword.end, JUMP, targetLabel));
      }
      case MODULE: {
        return valid(new LoadCellInstr(keyword.start, keyword.end, LOADMOD, -1));
      }
      case SELF: {
        final IdentifierInfo info = parentContext.getVariable(TokenType.SELF.name().toLowerCase());

        if(info == null) {
          return invalid(new ValidationException("'self' is unfound.", keyword.start, keyword.end));
        }

        return valid(info.getLoadInstr());
      }
      default: invalid(new ValidationException("Unkown keyword '"+keyword.repr()+"'.", keyword.start, keyword.end));
    }

    //This should never happen.
    return null;
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
    return parenthesized.getInner().accept(this, parentContext);
  }

  @Override
  public NodeResult visitObjectLiteral(CompContext parentContext, ObjectLiteral objectLiteral) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    /*
     * Make an argument vector to pass object attributes
     */
    instructions.add(new NoArgInstr(objectLiteral.start, objectLiteral.end, MAKEARGV));

    /*
     * New way of instiating object literals:
     * 
     * We essentially treat it as a function call. Key-value pairs are passed on the operand stack
     * using an ArgVector. We then use the the "allocos" instruction to properly setup this object
     * 
     * If the object literal is empty, we just use "alloco" for an empty object
     */
    for (Entry<String, Parameter> attr: objectLiteral.getAttributes().entrySet()) {
      final Parameter attrParam = attr.getValue();
      final int attrNameIndex = constantPool.addComponent(new StringConstant(attr.getKey()));

      final NodeResult valueRes = attrParam.getInitValue().accept(this, parentContext);
      if(valueRes.hasExceptions()) {
        exceptions.addAll(valueRes.getExceptions());
      }
      else {
        instructions.addAll(valueRes.getInstructions());
        instructions.add(new ArgInstr(attrParam.start, attrParam.end, ARG, attrNameIndex));
      }
    }

    instructions.add(new NoArgInstr(objectLiteral.start, objectLiteral.end, ALLOCO));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitCall(CompContext parentContext, FuncCall funcCall) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    /*
     * Make an argument vector to pass function arguments
     */
    instructions.add(new NoArgInstr(funcCall.start, funcCall.end, MAKEARGV));

    for(Argument arg : funcCall.getArguments()) {
      final NodeResult result = arg.getArgument().accept(this, parentContext);

      if (result.hasExceptions()) {
        exceptions.addAll(result.getExceptions());
      }
      else {
        /**
         * If the argument isn't geared towards an optional parameter,
         * the argNameIndex is -1, signaling that it's a positional argument.
         */
        final int argNameIndex = arg.hasName() ? 
                                    constantPool.addComponent(new StringConstant(arg.getParamName().getIdentifier())) : 
                                    -1;

        instructions.add(new ArgInstr(arg.getArgument().start, 
                                      arg.getArgument().end, 
                                      ARG, 
                                      argNameIndex));
      }
    }

    final NodeResult targetResult = funcCall.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
      instructions.add(new NoArgInstr(funcCall.start, funcCall.end, CALL));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitAttrAccess(CompContext parentContext, AttrAccess attrAccess) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Add instructions for target first
    final NodeResult targetResult = attrAccess.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }

    /*
     * Allocate attribute name in the constant pool. 
     */
    final int attrNameIndex = constantPool.addComponent(new StringConstant(attrAccess.getAttrName().getIdentifier()));
    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(new StoreCellInstr(attrAccess.start, attrAccess.end, STOREATTR, attrNameIndex));
    }
    else {
      instructions.add(new LoadCellInstr(attrAccess.start, attrAccess.end, LOADATTR, attrNameIndex));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitArray(CompContext parentContext, ArrayLiteral arrayLiteral) {
    final ConstantPool constantPool = parentContext.getConstantPool();
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    /*
     * Make argument vector for array elements 
     */
    instructions.add(new NoArgInstr(arrayLiteral.start, arrayLiteral.end, MAKEARGV));

    /*
     * Pass each array value as an argument to the arg vector
     */
    for (Node value : arrayLiteral.getValues()) {
      final NodeResult valueResult = value.accept(this, parentContext);
      if (valueResult.hasExceptions()) {
        exceptions.addAll(valueResult.getExceptions());
      }
      else {
        instructions.addAll(valueResult.getInstructions());
        instructions.add(new ArgInstr(value.start, value.end, ARG, -1));
      }
    }

    /**
     * Allocate the array, given the argument vector
     */
    instructions.add(new NoArgInstr(arrayLiteral.start, arrayLiteral.end, ALLOCA));

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitIndexAccess(CompContext parentContext, IndexAccess arrayAccess) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Add instructions for target first
    final NodeResult targetResult = arrayAccess.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }

    /*
     * Compile index expression
     */
    final NodeResult indexResult = arrayAccess.getIndex().accept(this, parentContext);
    if (indexResult.hasExceptions()) {
      exceptions.addAll(indexResult.getExceptions());
    }
    else {
      instructions.addAll(indexResult.getInstructions());
    }

    /*
     * If this is an assignment/storage operation, use STOREATTR 
     */
    if ((boolean) parentContext.getValue(ContextKey.NEED_STORAGE)) {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, STOREIN));
    }
    else {
      instructions.add(new NoArgInstr(arrayAccess.start, arrayAccess.end, LOADIN));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

  @Override
  public NodeResult visitVarDeclr(CompContext parentContext, VarDeclr varDeclr) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitVarDeclr'");
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    final List<Instruction> instructions = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    //Compile target expression first
    final NodeResult targetResult = unaryExpr.getTarget().accept(this, parentContext);
    if (targetResult.hasExceptions()) {
      exceptions.addAll(targetResult.getExceptions());
    }
    else {
      instructions.addAll(targetResult.getInstructions());
    }


    //Then add instruction for unary operator
    final Operator op = unaryExpr.getOperator();
    switch (op.getOp()) {
      case MINUS: {
        instructions.add(new NoArgInstr(unaryExpr.start, unaryExpr.end, NEG));
        break;
      }
      case BANG: {
        instructions.add(new NoArgInstr(unaryExpr.start, unaryExpr.end, NOT));
        break;
      }
      default: invalid(new ValidationException("'"+op.getOp().str+"' is an invalid unary operator.", 
                                               op.start, 
                                               op.end));
    }

    return exceptions.isEmpty() ? valid(instructions) : invalid(exceptions);
  }

}
