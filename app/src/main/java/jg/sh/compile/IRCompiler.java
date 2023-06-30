package jg.sh.compile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

import jg.sh.common.OperatorKind;
import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompContext.ContextType;
import jg.sh.compile.CompContext.IdentifierInfo;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.OpCode;
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
    return valid();
  }

  @Override
  public NodeResult visitFuncDef(CompContext parentContext, FuncDef funcDef) {
    // TODO Auto-generated method stub
    throw new UnsupportedOperationException("Unimplemented method 'visitFuncDef'");
  }

  @Override
  public NodeResult visitOperator(CompContext parentContext, Operator operator) {
    return valid();
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    final ConstantPool pool = parentContext.getConstantPool();
    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();
    final Op op = binaryOpExpr.getOperator().getOp();

    if(op == Op.ASSIGNMENT) {
      /*
       * Compile value expression first
       */
      final NodeResult valResult = binaryOpExpr.getRight().accept(this, parentContext);
      if(valResult.hasExceptions()) {
        exceptions.addAll(valResult.getExceptions());
      }
      else {
        instrs.addAll(valResult.getInstructions());
      }

      final CompContext leftContext = new CompContext(parentContext, parentContext.getCurrentContext());
      leftContext.setContextValue(ContextKey.NEED_STORAGE, true);

      /*
       * Compile assignee next.
       */
      final NodeResult assigneeRes = binaryOpExpr.getRight().accept(this, leftContext);
      if(assigneeRes.hasExceptions()) {
        exceptions.addAll(assigneeRes.getExceptions());
      }
      else {
        instrs.addAll(assigneeRes.getInstructions());
      }
    }
    else if(Op.mutatesLeft(op)) {
      /*
       * Expand expression, from a += b to a = a * b
       * where * is any operator
       */

      final BinaryOpExpr valueExpr = new BinaryOpExpr(binaryOpExpr.getLeft(), 
                                                      binaryOpExpr.getRight(), 
                                                      new Operator(Op.getMutatorOperator(op), 
                                                                   binaryOpExpr.getOperator().start, 
                                                                   binaryOpExpr.getOperator().end));
      final BinaryOpExpr assignExpr = new BinaryOpExpr(binaryOpExpr.getLeft(), 
                                                       valueExpr, 
                                                       new Operator(Op.ASSIGNMENT, 
                                                                    binaryOpExpr.getOperator().start, 
                                                                    binaryOpExpr.getOperator().end));
      return assignExpr.accept(this, parentContext);
    }
    else if(op == Op.BOOL_AND) {
      final String operandFalse = genLabelName("sc_op_false");
      final String endBranch =  genLabelName("sc_done");

      /*
       * If the left operand is false, jump to operandFalse
       */
      final NodeResult left = binaryOpExpr.getLeft().accept(this, parentContext);
      if (left.hasExceptions()) {
        exceptions.addAll(left.getExceptions());
      }
      else {
        instrs.addAll(left.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPF, operandFalse));
      }

      /**
       * At this point, the left operand must have evaulated to true.
       * Given that, let's evaluate the right operand. If that evaluates to false,
       * jump to operandFalse
       */
      final NodeResult right = binaryOpExpr.getRight().accept(this, parentContext);
      if (right.hasExceptions()) {
        exceptions.addAll(right.getExceptions());
      }
      else {
        instrs.addAll(right.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPF, operandFalse));
      }

      /*
       * At this point, both operands are true. Jump to endBranch
       */
      instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMP, endBranch));

      //operandFalse label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, operandFalse));
      final int falseConstant = pool.addComponent(new BoolConstant(false));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant));

      //endBranch label end
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, endBranch));
    }
    else if(op == Op.BOOL_OR) {
      final String operandTrue = genLabelName("sc_op_true");
      final String endBranch =  genLabelName("sc_done");

      /*
       * If the left operand is true, jump to operandTrue
       */
      final NodeResult left = binaryOpExpr.getLeft().accept(this, parentContext);
      if (left.hasExceptions()) {
        exceptions.addAll(left.getExceptions());
      }
      else {
        instrs.addAll(left.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPT, operandTrue));
      }

      /**
       * At this point, the left operand must have evaulated to false.
       * Given that, let's evaluate the right operand. If that evaluates to true,
       * jump to operandTrue
       */
      final NodeResult right = binaryOpExpr.getRight().accept(this, parentContext);
      if (right.hasExceptions()) {
        exceptions.addAll(right.getExceptions());
      }
      else {
        instrs.addAll(right.getInstructions());
        instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMPT, operandTrue));
      }

      //At this point, neither operand is true. Push true and jump to endBranch
      final int falseConstant = pool.addComponent(new BoolConstant(false));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, falseConstant));
      instrs.add(new JumpInstr(binaryOpExpr.start, binaryOpExpr.end, JUMP, endBranch));

      //operandTrue label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, operandTrue));
      final int trueConstantAddr = pool.addComponent(new BoolConstant(true));
      instrs.add(new ArgInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, trueConstantAddr));

      //endBranch label start
      instrs.add(new LabelInstr(binaryOpExpr.start, binaryOpExpr.end, endBranch));
    }
    else if(op == Op.ARROW) {
      /**
       * targetExpr -> newSelf (changes what "self" is for the targetExpr, which is expected to be a function)
       * 
       * Internally, this is call to system.bind()
       */
      instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, MAKEARGV));

      /**
       * Compile left operand first.
       */
      final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
      if (leftResult.hasExceptions()) {
        exceptions.addAll(leftResult.getExceptions());
      }
      else {
        instrs.addAll(leftResult.getInstructions());
        instrs.add(new ArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, ARG, -1));
      }
            
      /**
       * Compile the right operand next
       */
      final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);
      if (rightResult.hasExceptions()) {
        exceptions.addAll(rightResult.getExceptions());
      }
      else {
        instrs.addAll(rightResult.getInstructions());
        instrs.add(new ArgInstr(binaryOpExpr.getRight().start, binaryOpExpr.getRight().end, ARG, -1));
      }
      
      final int systemModuleName = pool.addComponent(new StringConstant("system"));
      final int bindName = pool.addComponent(new StringConstant("bind"));
      
      /**
       * Call system.bind()
       */
      instrs.add(new LoadCellInstr(binaryOpExpr.start, binaryOpExpr.end, LOADMV, systemModuleName));
      instrs.add(new LoadCellInstr(binaryOpExpr.start, binaryOpExpr.end, LOADATTR, bindName));
      instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, CALL));
    }
    else {
      /**
       * Compile left operand first.
       */
      final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
      if (leftResult.hasExceptions()) {
        exceptions.addAll(leftResult.getExceptions());
      }
      else {
        instrs.addAll(leftResult.getInstructions());
      }
            
      /**
       * Compile the right operand next
       */
      final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);
      if (rightResult.hasExceptions()) {
        exceptions.addAll(rightResult.getExceptions());
      }
      else {
        instrs.addAll(rightResult.getInstructions());
      }

      final OpCode opCode = opToCode(op);
      if (opCode == null) {
        exceptions.add(new ValidationException("'"+op.str+"' is an unknown operator.", 
                                               binaryOpExpr.getOperator().start, 
                                               binaryOpExpr.getOperator().end));
      }
      else {
        instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, opCode));
      }
    }

    return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions);
  }

  private static OpCode opToCode(Op op) {
    switch (op) {
      case PLUS: return ADD;
      case MINUS: return SUB;
      case MULT: return MUL;
      case DIV: return DIV;
      case MOD: return MOD;
      case LESS: return LESS;
      case GREAT: return GREAT;
      case GR_EQ: return GREATE;
      case LS_EQ: return LESSE;
      case EQUAL: return EQUAL;
      case NOT_EQ: return NOTEQUAL;
      case AND: return BAND;
      case OR: return BOR;
      default: return null;
    }
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

  //Utility methods - START
  private static long labelTag = 0;
  
  private static String genLabelName(String labelName) {
    String ret = labelName+labelTag;
    labelTag++;
    return ret;
  }
  //Utility methods - END
}
