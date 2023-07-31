package jg.sh.compile.optimization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.util.Pair;
import jg.sh.common.Location;
import jg.sh.compile.CompContext;
import jg.sh.compile.CompContext.ContextKey;
import jg.sh.compile.CompilerResult;
import jg.sh.compile.IRCompiler;
import jg.sh.compile.ObjectFile;
import jg.sh.compile.exceptions.ValidationException;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.optimization.targets.BooleanTarget;
import jg.sh.compile.optimization.targets.FloatTarget;
import jg.sh.compile.optimization.targets.IntegerTarget;
import jg.sh.compile.optimization.targets.OptimizableTarget;
import jg.sh.compile.optimization.targets.StringTarget;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.CodeObject;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.pool.component.StringConstant;
import jg.sh.compile.results.ConstantResult;
import jg.sh.compile.results.NodeResult;
import jg.sh.compile.results.VarResult;
import jg.sh.parsing.Module;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.Identifier;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Op;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.statements.VarDeclr;
import jg.sh.parsing.nodes.values.Bool;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;
import jg.sh.parsing.nodes.values.Str;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static jg.sh.compile.instrs.OpCode.*;
import static jg.sh.compile.results.NodeResult.*;
import static jg.sh.compile.optimization.OptimizationUtils.*;

public class OptimizingIRCompiler extends IRCompiler {

  private static final Logger LOG = LogManager.getLogger(OptimizingIRCompiler.class);

  @Override
  public CompilerResult compileModule(Module module) {
    CompilerResult result = super.compileModule(module);
    if (result.isSuccessful()) {
      //Shrink/squash the constant pool
      final ObjectFile obj = result.getObjectFile();
      final List<PoolComponent> pool = obj.getConstants();

      LOG.info("Success: "+obj);

      final List<Instruction> masterListInstrs = new ArrayList<>();
      masterListInstrs.addAll(obj.getModuleInstrs());
      masterListInstrs.addAll(pool.stream()
                                  .filter(x -> x instanceof CodeObject)
                                  .map(x -> ((CodeObject) x).getInstrs())
                                  .flatMap(Collection::stream).collect(Collectors.toList()));

      LOG.info("ALL instrs: "+masterListInstrs.stream().map(Instruction::toString).collect(Collectors.joining(System.lineSeparator())));      

      int newListIndex = 0;
      final ArrayList<Pair<PoolComponent, Integer>> newPoolList = new ArrayList<>();
      final HashSet<Integer> oldIndices = new HashSet<>();

      for (Instruction instruction : masterListInstrs) {
        if (OpCode.reliesOnConstantPool(instruction.getOpCode())) {
          final ArgInstr argInstr = (ArgInstr) instruction;
          final int poolIndex = argInstr.getArgument().getIndex();
          //System.out.println(" ===> INDICES: "+poolIndex+" || "+oldIndices+" || "+newListIndex);
          if (poolIndex >= 0 && !oldIndices.contains(poolIndex)) {
            final PoolComponent oldComponent = pool.get(poolIndex);
            //System.out.println("   ==> ADDED!!! "+poolIndex+" || "+oldComponent+" || "+instruction);
            
            newPoolList.add(new Pair<>(oldComponent, newListIndex++));
            oldIndices.add(poolIndex);
          }
        }
      }

      //Clear pool of its
      pool.clear();
      pool.addAll(
        newPoolList.stream().map(pair -> {
          pair.getKey().getIndex().setIndex(pair.getValue());
          return pair.getKey();
        }).collect(Collectors.toList()));
    }
    return result;
  }
  
  @Override
  public ConstantResult visitInt(CompContext parentContext, Int integer) {
    final ConstantResult result = super.visitInt(parentContext, integer);
    result.setTarget(new IntegerTarget(integer.getValue()));
    return result;
  }

  @Override
  public ConstantResult visitFloat(CompContext parentContext, FloatingPoint floatingPoint) {
    final ConstantResult result = super.visitFloat(parentContext, floatingPoint);
    result.setTarget(new FloatTarget(floatingPoint.getValue()));
    return result;
  }

  @Override
  public ConstantResult visitBoolean(CompContext parentContext, Bool bool) {
    final ConstantResult result = super.visitBoolean(parentContext, bool);
    result.setTarget(new BooleanTarget(bool.getValue()));
    return result;
  }

  @Override
  public ConstantResult visitString(CompContext parentContext, Str str) {
    final ConstantResult result = super.visitString(parentContext, str);
    result.setTarget(new StringTarget(str.getValue()));
    return result;
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    LOG.info(" => UNARY Optimize visit: "+unaryExpr);
    final NodeResult result = unaryExpr.getTarget().accept(this, parentContext);

    if (result.isOptimizable()) {
      final Operator op = unaryExpr.getOperator();
      final ConstantPool pool = parentContext.getConstantPool();

      final OptimizableTarget<?> target = result.getOptimizableTarget();

      LOG.info("UNARY IS OPTIMIZABLE: "+unaryExpr+" | "+target);

      if (target.getValue() instanceof Boolean) {
        final boolean targetBool = (Boolean) target.getValue();
        if (op.getOp() == Op.BANG) {
          final BooleanTarget negated = new BooleanTarget(!targetBool);
          //pool.removeComponent(targetBool.getExactIndex());

          LOG.info(" ==> UNARY optimizable result: "+negated);
          final LoadInstr loadConstant = new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getPoolComponent(pool).getIndex());
          return valid(loadConstant).setTarget(new BooleanTarget(negated.getValue()));
        }
      }
      else if (target.getValue() instanceof Long) {
        final long targetInt = (Long) target.getValue();
        if (op.getOp() == Op.MINUS) {
          final IntegerConstant negated = pool.addInt(-targetInt);
          //pool.removeComponent(targetInt.getExactIndex());

          LOG.info(" ==> UNARY optimizable result: "+negated);
          final LoadInstr loadConstant = new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getIndex());
          return valid(loadConstant).setTarget(new IntegerTarget(negated.getValue()));
        }
      }
      else {
        LOG.info("UNARY unknown OPTIMIZABLE type: "+target.getValue().getClass());
      }
    }

    LOG.info(" =========== UNARY, NOT OPTIMIZABLE "+unaryExpr);
    return super.visitUnary(parentContext, unaryExpr);
  }

  @Override
  public VarResult visitVarDeclr(CompContext parentContext, VarDeclr varDeclr) { 
    if (varDeclr.isConst()) {
      final NodeResult valueResult = varDeclr.getInitialValue().accept(this, parentContext);
      if (valueResult.isOptimizable()) {
        final OptimizableTarget<?> target = valueResult.getOptimizableTarget();
        parentContext.setConstant(varDeclr.getName().getIdentifier(), target);
      }
    }

    final VarResult result = super.visitVarDeclr(parentContext, varDeclr);
    return result;
  }

  @Override
  public NodeResult visitIdentifier(CompContext parentContext, Identifier identifier) {
    final NodeResult result = super.visitIdentifier(parentContext, identifier);

    if (parentContext.hasConstant(identifier.getIdentifier())) {
      result.setTarget(parentContext.getConstant(identifier.getIdentifier()));
    }

    return result;
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    LOG.info(" => BINOP Optimize visit: "+binaryOpExpr);

    final Op op = binaryOpExpr.getOperator().getOp();

    /*
     * Expand expression, from a *= b to a = a * b
     * where * is any operator
     */
    if (op == Op.ASSIGNMENT) {
      return super.visitBinaryExpr(parentContext, binaryOpExpr);
    }
    else if (Op.mutatesLeft(op) && op != Op.ASSIGNMENT) {
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
      return visitBinaryExpr(parentContext, assignExpr);
    }

    final ConstantPool pool = parentContext.getConstantPool();
    final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
    final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);

    final List<Instruction> instrs = new ArrayList<>();
    final List<ValidationException> exceptions = new ArrayList<>();

    if (leftResult.isOptimizable() && !rightResult.isOptimizable()) {
      if(op == Op.PLUS) {
        if (isTarget(leftResult.getOptimizableTarget(), 1) || isTarget(leftResult.getOptimizableTarget(), 1.0)) 
        {
          /**
           * Compile the right operand next
           */
          rightResult.pipeErr(exceptions).pipeInstr(instrs);

          instrs.add(new NoArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, INC));

          return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
        }
      }
      else if(op == Op.MINUS) {
        if (isTarget(leftResult.getOptimizableTarget(), 1) || isTarget(leftResult.getOptimizableTarget(), 1.0))
        {
          /**
           * Compile the right operand next
           */
          rightResult.pipeErr(exceptions).pipeInstr(instrs);

          instrs.add(new NoArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, DEC));

          return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
        } 
      }

      instrs.add(new LoadInstr(binaryOpExpr.getLeft().start, 
                                binaryOpExpr.getLeft().end, 
                                LOADC, 
                                leftResult.getOptimizableTarget().getPoolComponent(pool).getIndex()));
            
      /**
       * Compile the right operand next
       */
      rightResult.pipeErr(exceptions).pipeInstr(instrs);


      final OpCode opCode = opToCode(op);
      if (opCode == null) {
        exceptions.add(new ValidationException("'"+op.str+"' is an unknown operator.", 
                                              binaryOpExpr.getOperator().start, 
                                              binaryOpExpr.getOperator().end));
      }
      else {
        instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, opCode));
      }

      return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
      
    }
    else if(!leftResult.isOptimizable() && rightResult.isOptimizable()) {
      if(op == Op.PLUS ) {
          if (isTarget(rightResult.getOptimizableTarget(), 1) || isTarget(rightResult.getOptimizableTarget(), 1.0)) 
          {
            /**
             * Compile the right operand next
             */
            leftResult.pipeErr(exceptions).pipeInstr(instrs);

            instrs.add(new NoArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, INC));

            return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
          }
      }
      else if(op == Op.MINUS) {
        if (isTarget(rightResult.getOptimizableTarget(), 1) || isTarget(rightResult.getOptimizableTarget(), 1.0)) 
        {
          /**
           * Compile the right operand next
           */
          leftResult.pipeErr(exceptions).pipeInstr(instrs);

          instrs.add(new NoArgInstr(binaryOpExpr.getLeft().start, binaryOpExpr.getLeft().end, DEC));

          return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
        }
      }
      
      leftResult.pipeErr(exceptions).pipeInstr(instrs);

      instrs.add(new LoadInstr(binaryOpExpr.getOperator().start, 
                                binaryOpExpr.getRight().end, 
                                LOADC, 
                                rightResult.getOptimizableTarget().getPoolComponent(pool).getIndex()));

      final OpCode opCode = opToCode(op);
      if (opCode == null) {
        exceptions.add(new ValidationException("'"+op.str+"' is an unknown operator.", 
                                              binaryOpExpr.getOperator().start, 
                                              binaryOpExpr.getOperator().end));
      }
      else {
        instrs.add(new NoArgInstr(binaryOpExpr.start, binaryOpExpr.end, opCode));
      }

      return exceptions.isEmpty() ? valid(instrs) : invalid(exceptions); 
    }
    else if (leftResult.isOptimizable() && rightResult.isOptimizable()) {
      final OptimizableTarget<?> leftTarget = leftResult.getOptimizableTarget();
      final OptimizableTarget<?> rightTarget = rightResult.getOptimizableTarget();

      LOG.info("IS OPTIMIZABLE!!! "+binaryOpExpr+" | "+leftTarget+" "+rightTarget);

      if (op == Op.PLUS && (leftTarget instanceof StringTarget || rightTarget instanceof StringTarget)) {
        final StringTarget calcResult = new StringTarget(leftTarget.getValue().toString() + rightTarget.getValue().toString());
        final StringConstant poolComponent = pool.addString(calcResult.getValue());
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(calcResult);
      }
      else if (leftTarget instanceof IntegerTarget && rightTarget instanceof IntegerTarget) {
        final IntegerTarget leftInt = (IntegerTarget) leftTarget;
        final IntegerTarget rightInt = (IntegerTarget) rightTarget;

        OptimizableTarget<?> component = null;

        switch (op) {
          case PLUS: {
            component = new IntegerTarget(leftInt.getValue() + rightInt.getValue());
            break;
          }
          case MINUS: {
            component = new IntegerTarget(leftInt.getValue() - rightInt.getValue());
            break;
          }
          case MULT: {
            component = new IntegerTarget(leftInt.getValue() * rightInt.getValue());
            break;
          }
          case DIV: {
            component = new IntegerTarget(leftInt.getValue() / rightInt.getValue());
            break;
          }
          case MOD: {
            component = new IntegerTarget(leftInt.getValue() % rightInt.getValue());
            break;
          }
          case LESS: {
            component = new BooleanTarget(leftInt.getValue() < rightInt.getValue());
            break;
          }
          case GREAT: {
            component = new BooleanTarget(leftInt.getValue() > rightInt.getValue());
            break;
          }
          case GR_EQ: {
            component = new BooleanTarget(leftInt.getValue() >= rightInt.getValue());
            break;
          }
          case LS_EQ: {
            component = new BooleanTarget(leftInt.getValue() <= rightInt.getValue());
            break;
          }
          case EQUAL: {
            component = new BooleanTarget(leftInt.getValue() == rightInt.getValue());
            break;
          }
          case NOT_EQ: {
            component = new BooleanTarget(leftInt.getValue() != rightInt.getValue());
            break;
          }
          case AND: {
            component = new IntegerTarget(leftInt.getValue() & rightInt.getValue());
            break;
          }
          case OR: {
            component = new IntegerTarget(leftInt.getValue() | rightInt.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (II):  "+binaryOpExpr.repr()+" = "+component);

        final PoolComponent poolComponent = component.getPoolComponent(pool);
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(component);
      }
      else if (leftTarget instanceof IntegerTarget && rightTarget instanceof FloatTarget) {
        final IntegerTarget leftInt = (IntegerTarget) leftTarget;
        final FloatTarget rightFloat = (FloatTarget) rightTarget;

        OptimizableTarget<?> component = null;

        switch (op) {
          case PLUS: {
            component = new FloatTarget(leftInt.getValue() + rightFloat.getValue());
            break;
          }
          case MINUS: {
            component = new FloatTarget(leftInt.getValue() - rightFloat.getValue());
            break;
          }
          case MULT: {
            component = new FloatTarget(leftInt.getValue() * rightFloat.getValue());
            break;
          }
          case DIV: {
            component = new FloatTarget(leftInt.getValue() / rightFloat.getValue());
            break;
          }
          case MOD: {
            component = new FloatTarget(leftInt.getValue() % rightFloat.getValue());
            break;
          }
          case LESS: {
            component = new BooleanTarget(leftInt.getValue() < rightFloat.getValue());
            break;
          }
          case GREAT: {
            component = new BooleanTarget(leftInt.getValue() > rightFloat.getValue());
            break;
          }
          case GR_EQ: {
            component = new BooleanTarget(leftInt.getValue() >= rightFloat.getValue());
            break;
          }
          case LS_EQ: {
            component = new BooleanTarget(leftInt.getValue() <= rightFloat.getValue());
            break;
          }
          case EQUAL: {
            component = new BooleanTarget(((double) leftInt.getValue()) == rightFloat.getValue());
            break;
          }
          case NOT_EQ: {
            component = new BooleanTarget(((double) leftInt.getValue()) != rightFloat.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (IF): "+binaryOpExpr.repr()+" = "+component);

        final PoolComponent poolComponent = component.getPoolComponent(pool);
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(component);
      }
      else if (leftTarget instanceof FloatTarget && rightTarget instanceof IntegerTarget) {
        final FloatTarget leftFloat = (FloatTarget) leftTarget;
        final IntegerTarget rightInt = (IntegerTarget) rightTarget;

        OptimizableTarget<?> component = null;

        switch (op) {
          case PLUS: {
            component = new FloatTarget(leftFloat.getValue() + rightInt.getValue());
            break;
          }
          case MINUS: {
            component = new FloatTarget(leftFloat.getValue() - rightInt.getValue());
            break;
          }
          case MULT: {
            component = new FloatTarget(leftFloat.getValue() * rightInt.getValue());
            break;
          }
          case DIV: {
            component = new FloatTarget(leftFloat.getValue() / rightInt.getValue());
            break;
          }
          case MOD: {
            component = new FloatTarget(leftFloat.getValue() % rightInt.getValue());
            break;
          }
          case LESS: {
            component = new BooleanTarget(leftFloat.getValue() < rightInt.getValue());
            break;
          }
          case GREAT: {
            component = new BooleanTarget(leftFloat.getValue() > rightInt.getValue());
            break;
          }
          case GR_EQ: {
            component = new BooleanTarget(leftFloat.getValue() >= rightInt.getValue());
            break;
          }
          case LS_EQ: {
            component = new BooleanTarget(leftFloat.getValue() <= rightInt.getValue());
            break;
          }
          case EQUAL: {
            component = new BooleanTarget(leftFloat.getValue() == ((double) rightInt.getValue()));
            break;
          }
          case NOT_EQ: {
            component = new BooleanTarget(leftFloat.getValue() != ((double) rightInt.getValue()));
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (FI): "+binaryOpExpr.repr()+" = "+component);

        final PoolComponent poolComponent = component.getPoolComponent(pool);
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(component);
      }
      else if (leftTarget instanceof FloatTarget && rightTarget instanceof FloatTarget) {
        final FloatTarget leftFloat = (FloatTarget) leftTarget;
        final FloatTarget rightFloat = (FloatTarget) rightTarget;

        OptimizableTarget<?> component = null;

        switch (op) {
          case PLUS: {
            component = new FloatTarget(leftFloat.getValue() + rightFloat.getValue());
            break;
          }
          case MINUS: {
            component = new FloatTarget(leftFloat.getValue() - rightFloat.getValue());
            break;
          }
          case MULT: {
            component = new FloatTarget(leftFloat.getValue() * rightFloat.getValue());
            break;
          }
          case DIV: {
            component = new FloatTarget(leftFloat.getValue() / rightFloat.getValue());
            break;
          }
          case MOD: {
            component = new FloatTarget(leftFloat.getValue() % rightFloat.getValue());
            break;
          }
          case LESS: {
            component = new BooleanTarget(leftFloat.getValue() < rightFloat.getValue());
            break;
          }
          case GREAT: {
            component = new BooleanTarget(leftFloat.getValue() > rightFloat.getValue());
            break;
          }
          case GR_EQ: {
            component = new BooleanTarget(leftFloat.getValue() >= rightFloat.getValue());
            break;
          }
          case LS_EQ: {
            component = new BooleanTarget(leftFloat.getValue() <= rightFloat.getValue());
            break;
          }
          case EQUAL: {
            component = new BooleanTarget(leftFloat.getValue() == rightFloat.getValue());
            break;
          }
          case NOT_EQ: {
            component = new BooleanTarget(leftFloat.getValue() != rightFloat.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (FF): "+binaryOpExpr.repr()+" = "+component);

        final PoolComponent poolComponent = component.getPoolComponent(pool);
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(component);
      }
      else if (leftTarget instanceof BooleanTarget && rightTarget instanceof BooleanTarget) {
        final BooleanTarget leftBool = (BooleanTarget) leftTarget;
        final BooleanTarget rightBool = (BooleanTarget) rightTarget;

        OptimizableTarget<?> component = null;

        switch (op) {
          case BOOL_AND: {
            component = new BooleanTarget(leftBool.getValue() && rightBool.getValue());
            break;
          }
          case BOOL_OR: {
            component = new BooleanTarget(leftBool.getValue() || rightBool.getValue());
            break;
          }
          case OR: {
            component = new BooleanTarget(leftBool.getValue() | rightBool.getValue());
            break;
          }
          case AND: {
            component = new BooleanTarget(leftBool.getValue() & rightBool.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for boolean optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (BB): "+binaryOpExpr.repr()+" = "+component);

        final PoolComponent poolComponent = component.getPoolComponent(pool);
        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, poolComponent.getIndex());

        return valid(loadC).setTarget(component);
      }
      else {
        LOG.info("FAIL Optimization "+leftTarget.getClass()+" || "+rightTarget.getClass());
      }
    }
    
    return super.visitBinaryExpr(parentContext, binaryOpExpr);
  }
}
