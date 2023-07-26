package jg.sh.compile.optimization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.compile.CompContext;
import jg.sh.compile.IRCompiler;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.results.ConstantResult;
import jg.sh.compile.results.NodeResult;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;

import static jg.sh.compile.results.NodeResult.*;

import static jg.sh.compile.instrs.OpCode.*;

public class OptimizingIRCompiler extends IRCompiler {

  private static final Logger LOG = LogManager.getLogger(IRCompiler.class);
  
  @Override
  public ConstantResult visitInt(CompContext parentContext, Int integer) {
    final ConstantResult result = super.visitInt(parentContext, integer);
    result.setTarget(new OptimizableTarget(result.getConstant()));
    return result;
  }

  @Override
  public ConstantResult visitFloat(CompContext parentContext, FloatingPoint integer) {
    final ConstantResult result = super.visitFloat(parentContext, integer);
    result.setTarget(new OptimizableTarget(result.getConstant()));
    return result;
  }

  @Override
  public NodeResult visitUnary(CompContext parentContext, UnaryExpr unaryExpr) {
    final NodeResult result = super.visitUnary(parentContext, unaryExpr);
    if (result.isOptimizable()) {
      final Operator op = unaryExpr.getOperator();
      final ConstantPool pool = parentContext.getConstantPool();

      final OptimizableTarget target = result.getOptimizableTarget();
      if (target.getComponent() instanceof BoolConstant) {
        final BoolConstant targetBool = (BoolConstant) target.getComponent();
        if (op.getOp() == Op.BANG) {
          final BoolConstant negated = pool.addBoolean(!targetBool.getValue());
          return valid(negated.linkInstr(new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getExactIndex())));
        }
      }
      else if (target.getComponent() instanceof IntegerConstant) {
        final IntegerConstant targetInt = (IntegerConstant) target.getComponent();
        if (op.getOp() == Op.BANG) {
          final IntegerConstant negated = pool.addInt(-targetInt.getValue());
          return valid(negated.linkInstr(new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getExactIndex())));
        }
      }
    }

    return result;
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    final ConstantPool pool = parentContext.getConstantPool();
    final Op op = binaryOpExpr.getOperator().getOp();
    final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
    final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);

    if (leftResult.isOptimizable() && rightResult.isOptimizable()) {
      final OptimizableTarget leftTarget = leftResult.getOptimizableTarget();
      final OptimizableTarget rightTarget = rightResult.getOptimizableTarget();

      if (leftTarget.getComponent() instanceof IntegerConstant && rightTarget.getComponent() instanceof IntegerConstant) {
        final IntegerConstant leftInt = (IntegerConstant) leftTarget.getComponent();
        final IntegerConstant rightInt = (IntegerConstant) rightTarget.getComponent();

        PoolComponent component = null;

        switch (op) {
          case PLUS: {
            component = pool.addInt(leftInt.getValue() + rightInt.getValue());
            break;
          }
          case MINUS: {
            component = pool.addInt(leftInt.getValue() - rightInt.getValue());
            break;
          }
          case MULT: {
            component = pool.addInt(leftInt.getValue() * rightInt.getValue());
            break;
          }
          case DIV: {
            component = pool.addInt(leftInt.getValue() / rightInt.getValue());
            break;
          }
          case MOD: {
            component = pool.addInt(leftInt.getValue() % rightInt.getValue());
            break;
          }
          case LESS: {
            component = pool.addBoolean(leftInt.getValue() < rightInt.getValue());
            break;
          }
          case GREAT: {
            component = pool.addBoolean(leftInt.getValue() > rightInt.getValue());
            break;
          }
          case GR_EQ: {
            component = pool.addBoolean(leftInt.getValue() >= rightInt.getValue());
            break;
          }
          case LS_EQ: {
            component = pool.addBoolean(leftInt.getValue() <= rightInt.getValue());
            break;
          }
          case EQUAL: {
            component = pool.addBoolean(leftInt.getValue() == rightInt.getValue());
            break;
          }
          case NOT_EQ: {
            component = pool.addBoolean(leftInt.getValue() != rightInt.getValue());
            break;
          }
          case AND: {
            component = pool.addInt(leftInt.getValue() & rightInt.getValue());
            break;
          }
          case OR: {
            component = pool.addInt(leftInt.getValue() | rightInt.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        pool.removeComponent(leftInt.getExactIndex());
        pool.removeComponent(rightInt.getExactIndex());

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
      else if (leftTarget.getComponent() instanceof IntegerConstant && rightTarget.getComponent() instanceof FloatConstant) {
        final IntegerConstant leftInt = (IntegerConstant) leftTarget.getComponent();
        final FloatConstant rightFloat = (FloatConstant) rightTarget.getComponent();

        PoolComponent component = null;

        switch (op) {
          case PLUS: {
            component = pool.addFloat(leftInt.getValue() + rightFloat.getValue());
            break;
          }
          case MINUS: {
            component = pool.addFloat(leftInt.getValue() - rightFloat.getValue());
            break;
          }
          case MULT: {
            component = pool.addFloat(leftInt.getValue() * rightFloat.getValue());
            break;
          }
          case DIV: {
            component = pool.addFloat(leftInt.getValue() / rightFloat.getValue());
            break;
          }
          case MOD: {
            component = pool.addFloat(leftInt.getValue() % rightFloat.getValue());
            break;
          }
          case LESS: {
            component = pool.addBoolean(leftInt.getValue() < rightFloat.getValue());
            break;
          }
          case GREAT: {
            component = pool.addBoolean(leftInt.getValue() > rightFloat.getValue());
            break;
          }
          case GR_EQ: {
            component = pool.addBoolean(leftInt.getValue() >= rightFloat.getValue());
            break;
          }
          case LS_EQ: {
            component = pool.addBoolean(leftInt.getValue() <= rightFloat.getValue());
            break;
          }
          case EQUAL: {
            component = pool.addBoolean(((double) leftInt.getValue()) == rightFloat.getValue());
            break;
          }
          case NOT_EQ: {
            component = pool.addBoolean(((double) leftInt.getValue()) != rightFloat.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        pool.removeComponent(leftInt.getExactIndex());
        pool.removeComponent(rightFloat.getExactIndex());

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
      else if (leftTarget.getComponent() instanceof FloatConstant && rightTarget.getComponent() instanceof IntegerConstant) {
        final FloatConstant leftFloat = (FloatConstant) leftTarget.getComponent();
        final IntegerConstant rightInt = (IntegerConstant) rightTarget.getComponent();

        PoolComponent component = null;

        switch (op) {
          case PLUS: {
            component = pool.addFloat(leftFloat.getValue() + rightInt.getValue());
            break;
          }
          case MINUS: {
            component = pool.addFloat(leftFloat.getValue() - rightInt.getValue());
            break;
          }
          case MULT: {
            component = pool.addFloat(leftFloat.getValue() * rightInt.getValue());
            break;
          }
          case DIV: {
            component = pool.addFloat(leftFloat.getValue() / rightInt.getValue());
            break;
          }
          case MOD: {
            component = pool.addFloat(leftFloat.getValue() % rightInt.getValue());
            break;
          }
          case LESS: {
            component = pool.addBoolean(leftFloat.getValue() < rightInt.getValue());
            break;
          }
          case GREAT: {
            component = pool.addBoolean(leftFloat.getValue() > rightInt.getValue());
            break;
          }
          case GR_EQ: {
            component = pool.addBoolean(leftFloat.getValue() >= rightInt.getValue());
            break;
          }
          case LS_EQ: {
            component = pool.addBoolean(leftFloat.getValue() <= rightInt.getValue());
            break;
          }
          case EQUAL: {
            component = pool.addBoolean(leftFloat.getValue() == ((double) rightInt.getValue()));
            break;
          }
          case NOT_EQ: {
            component = pool.addBoolean(leftFloat.getValue() != ((double) rightInt.getValue()));
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        pool.removeComponent(leftFloat.getExactIndex());
        pool.removeComponent(rightInt.getExactIndex());

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
      else if (leftTarget.getComponent() instanceof FloatConstant && rightTarget.getComponent() instanceof FloatConstant) {
        final FloatConstant leftFloat = (FloatConstant) leftTarget.getComponent();
        final FloatConstant rightFloat = (FloatConstant) rightTarget.getComponent();

        PoolComponent component = null;

        switch (op) {
          case PLUS: {
            component = pool.addFloat(leftFloat.getValue() + rightFloat.getValue());
            break;
          }
          case MINUS: {
            component = pool.addFloat(leftFloat.getValue() - rightFloat.getValue());
            break;
          }
          case MULT: {
            component = pool.addFloat(leftFloat.getValue() * rightFloat.getValue());
            break;
          }
          case DIV: {
            component = pool.addFloat(leftFloat.getValue() / rightFloat.getValue());
            break;
          }
          case MOD: {
            component = pool.addFloat(leftFloat.getValue() % rightFloat.getValue());
            break;
          }
          case LESS: {
            component = pool.addBoolean(leftFloat.getValue() < rightFloat.getValue());
            break;
          }
          case GREAT: {
            component = pool.addBoolean(leftFloat.getValue() > rightFloat.getValue());
            break;
          }
          case GR_EQ: {
            component = pool.addBoolean(leftFloat.getValue() >= rightFloat.getValue());
            break;
          }
          case LS_EQ: {
            component = pool.addBoolean(leftFloat.getValue() <= rightFloat.getValue());
            break;
          }
          case EQUAL: {
            component = pool.addBoolean(leftFloat.getValue() == rightFloat.getValue());
            break;
          }
          case NOT_EQ: {
            component = pool.addBoolean(leftFloat.getValue() != rightFloat.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for arith optimization: "+op);
            break;
          }
        }

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        pool.removeComponent(leftFloat.getExactIndex());
        pool.removeComponent(rightFloat.getExactIndex());

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
    }

    return super.visitBinaryExpr(parentContext, binaryOpExpr);
  }
}
