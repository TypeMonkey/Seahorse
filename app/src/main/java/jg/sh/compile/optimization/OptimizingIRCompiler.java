package jg.sh.compile.optimization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.compile.CompContext;
import jg.sh.compile.CompilerResult;
import jg.sh.compile.IRCompiler;
import jg.sh.compile.ObjectFile;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.StoreInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.BoolConstant;
import jg.sh.compile.pool.component.CodeObject;
import jg.sh.compile.pool.component.FloatConstant;
import jg.sh.compile.pool.component.IntegerConstant;
import jg.sh.compile.pool.component.PoolComponent;
import jg.sh.compile.results.ConstantResult;
import jg.sh.compile.results.NodeResult;
import jg.sh.parsing.Module;
import jg.sh.parsing.nodes.BinaryOpExpr;
import jg.sh.parsing.nodes.Operator;
import jg.sh.parsing.nodes.Operator.Op;
import jg.sh.parsing.nodes.UnaryExpr;
import jg.sh.parsing.nodes.values.FloatingPoint;
import jg.sh.parsing.nodes.values.Int;

import static jg.sh.compile.results.NodeResult.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import static jg.sh.compile.instrs.OpCode.*;

public class OptimizingIRCompiler extends IRCompiler {

  private static final Logger LOG = LogManager.getLogger(OptimizingIRCompiler.class);

  @Override
  public CompilerResult compileModule(Module module) {
    CompilerResult result = super.compileModule(module);
    if (result.isSuccessful()) {
      //Shrink/squash the constant pool
      final ObjectFile obj = result.getObjectFile();
      final ConstantPool pool = obj.getPool();

      LOG.info("Success: "+obj);

      final List<Instruction> masterListInstrs = new ArrayList<>();
      masterListInstrs.addAll(obj.getModuleInstrs());
      masterListInstrs.addAll(pool.getMembers().stream()
                                               .filter(x -> x instanceof CodeObject)
                                               .map(x -> ((CodeObject) x).getInstrs())
                                               .flatMap(Collection::stream).collect(Collectors.toList()));

      LOG.info("ALL instrs: "+masterListInstrs.stream().map(Instruction::toString).collect(Collectors.joining(System.lineSeparator())));      

      int newListIndex = 0;
      final ArrayList<PoolComponent> newPoolList = new ArrayList<>();
      final HashSet<Integer> oldIndices = new HashSet<>();

      for (Instruction instruction : masterListInstrs) {
        if (OpCode.reliesOnConstantPool(instruction.getOpCode())) {
          final int poolIndex = instruction instanceof LoadInstr ? 
                                  ((LoadInstr) instruction).getIndex() : 
                                  (instruction instanceof StoreInstr ? 
                                    ((StoreInstr) instruction).getIndex() : 
                                    ((ArgInstr) instruction).getArgument());
          System.out.println(" ===> INDICES: "+poolIndex+" || "+oldIndices+" || "+newListIndex);
          if (poolIndex >= 0 && !oldIndices.contains(poolIndex)) {
            final PoolComponent oldComponent = pool.getComponent(poolIndex);
            
            newPoolList.add(oldComponent);
            oldComponent.getIndex().setIndex(newListIndex++);
            oldIndices.add(poolIndex);
          }
        }
      }

      pool.getMembers().clear();
      pool.getMembers().addAll(newPoolList);
    }
    return result;
  }

  
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
    LOG.info(" => UNARY Optimize visit: "+unaryExpr);
    final NodeResult result = unaryExpr.getTarget().accept(this, parentContext);

    if (result.isOptimizable()) {
      final Operator op = unaryExpr.getOperator();
      final ConstantPool pool = parentContext.getConstantPool();

      final OptimizableTarget target = result.getOptimizableTarget();

      LOG.info("UNARY IS OPTIMIZABLE: "+unaryExpr+" | "+target);

      if (target.getComponent() instanceof BoolConstant) {
        final BoolConstant targetBool = (BoolConstant) target.getComponent();
        if (op.getOp() == Op.BANG) {
          final BoolConstant negated = pool.addBoolean(!targetBool.getValue());
          //pool.removeComponent(targetBool.getExactIndex());

          LOG.info(" ==> UNARY optimizable result: "+negated);
          final LoadInstr loadConstant = new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getExactIndex());
          return valid(negated.linkInstr(loadConstant)).setTarget(new OptimizableTarget(negated));
        }
      }
      else if (target.getComponent() instanceof IntegerConstant) {
        final IntegerConstant targetInt = (IntegerConstant) target.getComponent();
        if (op.getOp() == Op.MINUS) {
          final IntegerConstant negated = pool.addInt(-targetInt.getValue());
          //pool.removeComponent(targetInt.getExactIndex());

          LOG.info(" ==> UNARY optimizable result: "+negated);
          final LoadInstr loadConstant = new LoadInstr(unaryExpr.start, unaryExpr.end, LOADC, negated.getExactIndex());
          return valid(negated.linkInstr(loadConstant)).setTarget(new OptimizableTarget(negated));
        }
      }
      else {
        LOG.info("UNARY unknown OPTIMIZABLE type: "+target.getComponent().getClass());
      }
    }

    LOG.info(" =========== UNARY, NOT OPTIMIZABLE "+unaryExpr);
    return super.visitUnary(parentContext, unaryExpr);
  }

  @Override
  public NodeResult visitBinaryExpr(CompContext parentContext, BinaryOpExpr binaryOpExpr) {
    LOG.info(" => BINOP Optimize visit: "+binaryOpExpr);

    final ConstantPool pool = parentContext.getConstantPool();
    final Op op = binaryOpExpr.getOperator().getOp();
    final NodeResult leftResult = binaryOpExpr.getLeft().accept(this, parentContext);
    final NodeResult rightResult = binaryOpExpr.getRight().accept(this, parentContext);

    if (leftResult.isOptimizable() && rightResult.isOptimizable()) {
      final OptimizableTarget leftTarget = leftResult.getOptimizableTarget();
      final OptimizableTarget rightTarget = rightResult.getOptimizableTarget();

      LOG.info("IS OPTIMIZABLE!!! "+binaryOpExpr+" | "+leftTarget.getComponent()+" "+rightTarget.getComponent());

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

        LOG.info(" ==> OPTIMIZABLE RESULT (II):  "+binaryOpExpr.repr()+" = "+component);

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        //pool.removeComponent(leftInt.getExactIndex());
        //pool.removeComponent(rightInt.getExactIndex());

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

        LOG.info(" ==> OPTIMIZABLE RESULT (IF): "+binaryOpExpr.repr()+" = "+component);

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        //pool.removeComponent(leftInt.getExactIndex());
        //pool.removeComponent(rightFloat.getExactIndex());

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

        LOG.info(" ==> OPTIMIZABLE RESULT (FI): "+binaryOpExpr.repr()+" = "+component);

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        //pool.removeComponent(leftFloat.getExactIndex());
        //pool.removeComponent(rightInt.getExactIndex());

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

        LOG.info(" ==> OPTIMIZABLE RESULT (FF): "+binaryOpExpr.repr()+" = "+component);

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        //pool.removeComponent(leftFloat.getExactIndex());
        //pool.removeComponent(rightFloat.getExactIndex());

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
      else if (leftTarget.getComponent() instanceof BoolConstant && rightTarget.getComponent() instanceof BoolConstant) {
        final BoolConstant leftBool = (BoolConstant) leftTarget.getComponent();
        final BoolConstant rightBool = (BoolConstant) rightTarget.getComponent();

        PoolComponent component = null;

        switch (op) {
          case BOOL_AND: {
            component = pool.addBoolean(leftBool.getValue() && rightBool.getValue());
            break;
          }
          case BOOL_OR: {
            component = pool.addBoolean(leftBool.getValue() || rightBool.getValue());
            break;
          }
          case OR: {
            component = pool.addBoolean(leftBool.getValue() | rightBool.getValue());
            break;
          }
          case AND: {
            component = pool.addBoolean(leftBool.getValue() & rightBool.getValue());
            break;
          }
          default: {
            LOG.warn("Unknown operator for boolean optimization: "+op);
            break;
          }
        }

        LOG.info(" ==> OPTIMIZABLE RESULT (BB): "+binaryOpExpr.repr()+" = "+component);

        final LoadInstr loadC = new LoadInstr(binaryOpExpr.start, binaryOpExpr.end, LOADC, component.getExactIndex());
        component.linkInstr(loadC);

        /*
        No need to remove PoolComponents for booleans.
        pool.removeComponent(leftFloat.getExactIndex());
        pool.removeComponent(rightFloat.getExactIndex());
        */

        return valid(loadC).setTarget(new OptimizableTarget(component));
      }
      else {
        LOG.info("FAIL Optimization "+leftTarget.getComponent().getClass()+" || "+rightTarget.getComponent().getClass());
      }
    }
    
    return super.visitBinaryExpr(parentContext, binaryOpExpr);
  }
}
