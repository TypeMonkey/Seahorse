package jg.sh.util;

import java.util.Set;

import com.google.common.collect.Sets;

import jg.sh.common.FunctionSignature;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.CommentInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.instrs.ArgInstruction;
import jg.sh.runtime.instrs.CommentInstruction;
import jg.sh.runtime.instrs.NoArgInstruction;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.loading.IndexedJumpInstr;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.ImmediateInternalCallable;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimePrimitive;
import jg.sh.runtime.threading.fiber.Fiber;

/**
 * Utility methods used during code execution
 */
public final class RuntimeUtils {

  public static CallSiteException checkArgs(Callable callable, FunctionSignature signature, ArgVector args) {
    //-2 from positional size as the first two arguments are self and the function itself
    if (args.getPositionals().size() - 2 < signature.getPositionalParamCount()) {
      return new CallSiteException("The function requires "+signature.getPositionalParamCount()+" positional arguments", callable);
    }
    if (args.getPositionals().size() - 2 > signature.getPositionalParamCount() && !signature.hasVariableParams()) {
      //System.out.println(" === minus 2: "+(args.getPositionals().size() - 2)+" | expected: "+signature.getPositionalParamCount());
      //System.out.println(" ===> "+args.getPositionals().stream().map(x -> x.getClass().getName()).collect(Collectors.joining(",")));
      //System.out.println(" ===> "+args.getPositionals());
      return new CallSiteException("Excess positional arguments. The function doesn't accept variable argument amount! "+args.getPositionals().size(), callable);
    }

    if(args.attrs().size() > 0 && !signature.hasVarKeywordParams()) {
      final Set<String> extraKeyswords = Sets.difference(args.attrs(), signature.getKeywordParams());
      if (extraKeyswords.size() > 0) {
        return new CallSiteException("Unknown keyword arguments '"+extraKeyswords+"'", callable);
      }
    }

    return null;
  }


  /**
   * Immediately executes an ImmediateInternal Callable
   * @param callable - the Callable to invoke
   * @param args - the ArgVector to pass to the Callable
   * @param currentFiber - the Fiber this Callable is to be executed on
   * @return the RuntimeInstance representing the return value of the Callable, or null if the Callable isn't an ImmediateInternalCallable
   * @throws InvocationException - throw if the Callable throws it
   * @throws CallSiteException - if the arguments in the ArgVector isn't compatible with the Callable's FunctionSignature
   */
  public static RuntimeInstance fastCall(Callable callable, ArgVector args, Fiber currentFiber) throws InvocationException, CallSiteException {
    if (callable instanceof ImmediateInternalCallable) {
      final ImmediateInternalCallable internalCallable = (ImmediateInternalCallable) callable;
      final FunctionSignature signature = internalCallable.getSignature();

      args.addAtFront(callable.getSelf());
      args.addAtFront(callable);

      final CallSiteException exception = checkArgs(internalCallable, signature, args);
      if (exception != null) {
        throw exception;
      }

      return internalCallable.getFunction().invoke(currentFiber, args);
    }
    return null;
  }

  public static RuntimePrimitive numAdd(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftFloat.getValue() + rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateFloat(leftFloat.getValue() + rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftLong.getValue() + rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateInt(leftLong.getValue() + rightLong.getValue());
    }

    return null;
  }

  public static RuntimePrimitive numMinus(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftFloat.getValue() - rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateFloat(leftFloat.getValue() - rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftLong.getValue() - rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateInt(leftLong.getValue() - rightLong.getValue());
    }

    return null;
  }

  public static RuntimePrimitive numDiv(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) throws ArithmeticException {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftFloat.getValue() / rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateFloat(leftFloat.getValue() / rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftLong.getValue() / rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateInt(leftLong.getValue() / rightLong.getValue());
    }

    return null;
  }

  public static RuntimePrimitive numMult(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftFloat.getValue() * rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateFloat(leftFloat.getValue() * rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftLong.getValue() * rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateInt(leftLong.getValue() * rightLong.getValue());
    }

    return null;
  }

  public static RuntimePrimitive numMod(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) throws ArithmeticException {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftFloat.getValue() % rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateFloat(leftFloat.getValue() % rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateFloat(leftLong.getValue() % rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateInt(leftLong.getValue() % rightLong.getValue());
    }

    return null;
  }

  public static RuntimeBool numLess(RuntimeInstance left, RuntimeInstance right, boolean orEqual, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(orEqual ? 
                                     leftFloat.getValue() <= rightFloat.getValue() : 
                                     leftFloat.getValue() < rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(orEqual ? 
                                     leftFloat.getValue() <= rightLong.getValue() : 
                                     leftFloat.getValue() < rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(orEqual ? 
                                     leftLong.getValue() <= rightFloat.getValue() : 
                                     leftLong.getValue() < rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(orEqual ? 
                                     leftLong.getValue() <= rightLong.getValue() : 
                                     leftLong.getValue() < rightLong.getValue());
    }

    return null;
  }

  public static RuntimeBool numGreat(RuntimeInstance left, RuntimeInstance right, boolean orEqual, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(orEqual ? 
                                     leftFloat.getValue() >= rightFloat.getValue() : 
                                     leftFloat.getValue() > rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(orEqual ? 
                                     leftFloat.getValue() >= rightLong.getValue() : 
                                     leftFloat.getValue() > rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(orEqual ? 
                                     leftLong.getValue() >= rightFloat.getValue() : 
                                     leftLong.getValue() > rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(orEqual ? 
                                     leftLong.getValue() >= rightLong.getValue() : 
                                     leftLong.getValue() > rightLong.getValue());
    }

    return null;
  }

  public static RuntimeBool numEqual(RuntimeInstance left, RuntimeInstance right, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(leftFloat.getValue() == rightFloat.getValue());
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(leftFloat.getValue() == rightLong.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      return allocator.allocateBool(leftLong.getValue() == rightFloat.getValue());
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      return allocator.allocateBool(leftLong.getValue() == rightLong.getValue());
    }

    return null;
  }

  public static RuntimeBool negate(RuntimeInstance operand, HeapAllocator allocator) {
    if(operand instanceof RuntimeBool) {
      return allocator.allocateBool(!((RuntimeBool) operand).getValue());
    }
    return null;
  }

  public static RuntimeInteger negative(RuntimeInstance operand, HeapAllocator allocator) {
    if(operand instanceof RuntimeInteger) {
      return allocator.allocateInt( - ((RuntimeInteger) operand).getValue());
    }
    return null;
  }
  
  public static boolean isPrimitive(RuntimeInstance instance) {
    return instance instanceof RuntimePrimitive;
  }

  public static boolean isNumerical(RuntimeInstance instance) {
    return instance instanceof RuntimeInteger || instance instanceof RuntimeFloat;
  }

  public static RuntimeInstruction translate(Instruction instruction, int exceptionJumpIndex) {
    if (instruction instanceof CommentInstr || instruction instanceof LabelInstr) {
      final String content = instruction instanceof CommentInstr ? 
                                ((CommentInstr) instruction).getContent() : 
                                ((LabelInstr) instruction).getName();
      return new CommentInstruction(exceptionJumpIndex, content)
                   .setStart(instruction.getStart())
                   .setEnd(instruction.getEnd());
    }
    else if(instruction instanceof ArgInstr) {
      final ArgInstr argInstr = (ArgInstr) instruction;
      return new ArgInstruction(argInstr.getOpCode(), argInstr.getArgument().getIndex(), exceptionJumpIndex)
                   .setStart(instruction.getStart())
                   .setEnd(instruction.getEnd());
    }
    else if(instruction instanceof NoArgInstr) {
      final NoArgInstr noArgInstr = (NoArgInstr) instruction;
      return new NoArgInstruction(noArgInstr.getOpCode(), exceptionJumpIndex)
                   .setStart(instruction.getStart())
                   .setEnd(instruction.getEnd());
    }
    else if(instruction instanceof IndexedJumpInstr) {
      final IndexedJumpInstr jumpInstr = (IndexedJumpInstr) instruction;
      return new ArgInstruction(jumpInstr.getOpCode(), jumpInstr.getJumpIndex(), exceptionJumpIndex)
                   .setStart(instruction.getStart())
                   .setEnd(instruction.getEnd());
    }
    throw new IllegalArgumentException("Can't translate instruction: "+instruction);
  } 
}
