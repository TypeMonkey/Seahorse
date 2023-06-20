package jg.sh.util;

import jg.sh.irgen.instrs.OpCode;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimePrimitive;

/**
 * Utility methods used during code execution
 */
public final class RuntimeUtils {

  /**
   * Executes arithmetic and comparative operators on primitive numerical types (int and float)
   * without invoking their respective operator functions.
   * 
   * @param left - the left operand
   * @param right - the right operand
   * @param op - the operation to perform
   * @param allocator - the HeapAllocator to use for the result
   * @return the result of the operator (as a RuntimePrimitive), 
   *         or null if neither operand is a float or int, or the operation isn't supported.
   */
  public static RuntimePrimitive fastNumArith(RuntimeInstance left, RuntimeInstance right, OpCode op, HeapAllocator allocator) {
    if (left instanceof RuntimeFloat && right instanceof RuntimeFloat) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      switch (op) {
        case ADD: return allocator.allocateFloat(leftFloat.getValue() + rightFloat.getValue());
        case SUB: return allocator.allocateFloat(leftFloat.getValue() - rightFloat.getValue());
        case MUL: return allocator.allocateFloat(leftFloat.getValue() * rightFloat.getValue());
        case DIV: return allocator.allocateFloat(leftFloat.getValue() / rightFloat.getValue());
        case MOD: return allocator.allocateFloat(leftFloat.getValue() % rightFloat.getValue());
        
        //Bitwise operator (not supported for floating point values)
        //case BAND: return allocator.allocateFloat(leftFloat.getValue() & rightFloat.getValue());
        //case BOR: return allocator.allocateFloat(leftFloat.getValue() | rightFloat.getValue());
          
        //Comparative operators
        case LESS: return allocator.allocateBool(leftFloat.getValue() < rightFloat.getValue());
        case GREAT: return allocator.allocateBool(leftFloat.getValue() > rightFloat.getValue());
        case LESSE: return allocator.allocateBool(leftFloat.getValue() <= rightFloat.getValue());
        case GREATE: return allocator.allocateBool(leftFloat.getValue() >= rightFloat.getValue());        
        case EQUAL: return allocator.allocateBool(leftFloat.getValue() == rightFloat.getValue());
        default: return null;
      }
    }
    else if (left instanceof RuntimeFloat && right instanceof RuntimeInteger) {
      final RuntimeFloat leftFloat = (RuntimeFloat) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      switch (op) {
        case ADD: return allocator.allocateFloat(leftFloat.getValue() + rightLong.getValue());
        case SUB: return allocator.allocateFloat(leftFloat.getValue() - rightLong.getValue());
        case MUL: return allocator.allocateFloat(leftFloat.getValue() * rightLong.getValue());
        case DIV: return allocator.allocateFloat(leftFloat.getValue() / rightLong.getValue());
        case MOD: return allocator.allocateFloat(leftFloat.getValue() % rightLong.getValue());
        
        //Bitwise operator (not supported for floating point values)
        //case BAND: return allocator.allocateFloat(leftFloat.getValue() & rightFloat.getValue());
        //case BOR: return allocator.allocateFloat(leftFloat.getValue() | rightFloat.getValue());
          
        //Comparative operators
        case LESS: return allocator.allocateBool(leftFloat.getValue() < rightLong.getValue());
        case GREAT: return allocator.allocateBool(leftFloat.getValue() > rightLong.getValue());
        case LESSE: return allocator.allocateBool(leftFloat.getValue() <= rightLong.getValue());
        case GREATE: return allocator.allocateBool(leftFloat.getValue() >= rightLong.getValue());
        case EQUAL: return allocator.allocateBool(leftFloat.getValue() == rightLong.getValue());             
        default: return null;
      }
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeFloat) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeFloat rightFloat = (RuntimeFloat) right;

      switch (op) {
        case ADD: return allocator.allocateFloat(leftLong.getValue() + rightFloat.getValue());
        case SUB: return allocator.allocateFloat(leftLong.getValue() - rightFloat.getValue());
        case MUL: return allocator.allocateFloat(leftLong.getValue() * rightFloat.getValue());
        case DIV: return allocator.allocateFloat(leftLong.getValue() / rightFloat.getValue());
        case MOD: return allocator.allocateFloat(leftLong.getValue() % rightFloat.getValue());
        
        //Bitwise operator (not supported for floating point values)
        //case BAND: return allocator.allocateFloat(leftFloat.getValue() & rightFloat.getValue());
        //case BOR: return allocator.allocateFloat(leftFloat.getValue() | rightFloat.getValue());
          
        //Comparative operators
        case LESS: return allocator.allocateBool(leftLong.getValue() < rightFloat.getValue());
        case GREAT: return allocator.allocateBool(leftLong.getValue() > rightFloat.getValue());
        case LESSE: return allocator.allocateBool(leftLong.getValue() <= rightFloat.getValue());
        case GREATE: return allocator.allocateBool(leftLong.getValue() >= rightFloat.getValue());
        case EQUAL: return allocator.allocateBool(leftLong.getValue() == rightFloat.getValue());                      
        default: return null;
      }
    }
    else if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      final RuntimeInteger leftLong = (RuntimeInteger) left;
      final RuntimeInteger rightLong = (RuntimeInteger) right;

      switch (op) {
        case ADD: return allocator.allocateInt(leftLong.getValue() + rightLong.getValue());
        case SUB: return allocator.allocateInt(leftLong.getValue() - rightLong.getValue());
        case MUL: return allocator.allocateInt(leftLong.getValue() * rightLong.getValue());
        case DIV: return allocator.allocateInt(leftLong.getValue() / rightLong.getValue());
        case MOD: return allocator.allocateInt(leftLong.getValue() % rightLong.getValue());
        
        //Bitwise operator (not supported for floating point values)
        case BAND: return allocator.allocateInt(leftLong.getValue() & rightLong.getValue());
        case BOR: return allocator.allocateInt(leftLong.getValue() | rightLong.getValue());
          
        //Comparative operators
        case LESS: return allocator.allocateBool(leftLong.getValue() < rightLong.getValue());
        case GREAT: return allocator.allocateBool(leftLong.getValue() > rightLong.getValue());
        case LESSE: return allocator.allocateBool(leftLong.getValue() <= rightLong.getValue());
        case GREATE: return allocator.allocateBool(leftLong.getValue() >= rightLong.getValue());      
        case EQUAL: return allocator.allocateBool(leftLong.getValue() == rightLong.getValue());                
        default: return null;
      }
    }
    return null;
  }
  
  public static boolean isPrimitive(RuntimeInstance instance) {
    return instance instanceof RuntimePrimitive;
  }

  public static boolean isNumerical(RuntimeInstance instance) {
    return instance instanceof RuntimeInteger || instance instanceof RuntimeFloat;
  }
}
