package jg.sh.compile.instrs;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Lists all the currently supported bytecode opcodes
 * for the SeaHorse Interpreter.
 * 
 * Development note: 
 *   The ordering of the OpCode as enums are important as the various
 *   isXXX() methods use the ordering as indexes to check whether
 *   a certain OpCode belongs to certain group.
 *   
 *   It's important to note that changes to this file must also 
 *   come with changes to those isXXX() methods.
 * @author Jose
 *
 */
public enum OpCode {
  LABEL,
  
  COMMENT,
  
  /*
   * Arithmetic operators
   */
  ADD,
  SUB,
  MUL,
  DIV,
  MOD,
  NEG,
  
  /*
   * Comparative operators
   */
  LESS,
  GREAT,
  LESSE,
  GREATE,
  EQUAL,  //Object equality
  REQUAL, //Reference equality
  NOTEQUAL,
  
  /*
   * Boolean operators.
   * 
   * Both AND and OR are compiled away to their explicit
   * instruction list equivalents
   */
  NOT,
  
  /*
   * Bitwise operators 
   */
  BAND,
  BOR,
  
  /*
   * Instruction jumps and function calls
   */
  JUMP,  //Jumps to the given label unconditionally. DOESN'T POP operand from operand stack
  
  /*
   * These other jump opcodes WILL POP the top operand from the operand stack.
   * 
   * However, it will repop the operand if the operand doesn't fit the condition
   * they're checking for
   */
  JUMPT, //Jumps to the given label if the value on top of the operand stack is 1
  JUMPF, //Jumps to the given label if the value on top of the operand stack is 0
  
  /*
   * Will treat the object on top of the operand stack as a function.
   * 
   * The return value returned by the function will be popped to the top of the operand stack
   * 
   * TOP    -> callable
   *         | argVector  <- should include "self"
   * BOTTOM ->
   * 
   */
  CALL,  //Will treat the object on top of the operand stack as a function
  
  /*
   * Treats the object on top of the operand stack as a data definition
   * and creates an empty object to pass to the constructor as "self"
   * 
   * This instruction also sets the instance functions of that empty object
   */
  @Deprecated
  CALLA, 
  
  
  RETE,  //Returns to caller, but with the exception flag to be true 
  RET,   //Returns to caller
  
  /*
   * Loads and stores a value from the function stack at a given offset
   */
  LOADC, //loads a constant from the constant pool
  
  /*
   * Loads and stores a local variable from the function stack
   * 
   * Both instructions require an index corresponding to the local variable
   */
  LOAD,
  STORE,
  
  /*
   * Loads the current error object to the operand stack
   */
  POPERR,
  
  /*
   * Loads a module instance.
   * 
   * This instruction requires an index corresponding to the name of the module to 
   * be loaded - on the constant pool.
   * 
   * If the index is a negative value, this instruction will load the current module
   */
  LOADMOD, 

  /*
   * Loads the "self" instance. 
   * 
   * If this instruction is used within a function not hosted within a class,
   * it will load the current module or the object that the current function is binded to.
   */
  @Deprecated
  LOADSELF,
  
  /*
   * Loads the attribute of the object currently on the operand stack.
   * 
   * This instruction requires an index which corresponds to the string attribute name on the constant pool
   * 
   * TOP    -> target
   *         |
   * BOTTOM ->
   */
  LOADATTR,
  
  /*
   * Stores the attribute of the object currently on the operand stack
   * 
   * This instruction requires an index which corresponds to the string attribute name on the constant pool
   * 
   * TOP    -> target
   *         | value
   * BOTTOM ->
   * 
   * This instruction re-pops the target object to the operand stack
   */
  STOREATTR,

  /*
   * Adds a modifier to an object's attribute.
   * 
   * This instruction requires an index which corresponds to the string attribute name on the constant pool
   * 
   * TOP    -> modifierCode
   *         | target
   * BOTTOM ->
   * 
   * This instruction re-pops the target object to the operand stack
   * 
   * Modifier code corresponds to a positive, non-zero integer. At the moment,
   * the Seahorse interpreter recoginizes the following modifier codes
   *  -> 1 = const (make the attribute non re-assignable)
   */
  SETDESC,
  
  /*
   * Pushes the null address (0) to the operand stack
   */
  LOADNULL,
  
  /*
   * Loads and stores a variable based on a function instance's
   * captured variable storage
   * 
   * Both instructions require an index that corresponds 
   * to the variable's index on the function instance's capture variable storage
   */
  LOAD_CL,
  STORE_CL,
  
  /*
   * Loads and stores a module-level (a.k.a "global") 
   * variable.
   * 
   * Both instructions require an index that corresponds 
   * to the string name in the constant pool
   */
  LOADMV, 
  STOREMV,
  
  /*
   * Exports a module variable for public use
   * 
   * This instruction requires an index that corresponds to the module variable's name
   * in the constant pool
   */
  EXPORTMV,
  
  /*
   * Makes a module variable constant
   * 
   * This instruction requires an index that corresponds to the module variable's name
   * in the constant pool
   */
  CONSTMV,
  
  /*
   * Creates an instance of a function from a code object at the top of the operand stack
   * 
   * TOP      -> self
   *           | codeObject
   * BOTTOM   ->
   * 
   * This instruction then pops the allocated callable function on the top of the operand stack
   */
  ALLOCF,
  
  /*
   * Sets a captured variable to an instance of a function (presumed to be at the top of the variable stack)
   * 
   * The instruction requires an index that corresponds to the index of this captured variable
   * 
   * This instruction re-pops the function instance at the top of the operand stack
   */
  @Deprecated
  CAPTURE,
  
  
  /*
   * Allocates an array and pushes it to the operand stack
   * 
   * TOP -> argVector
   *      |
   * BOTTOM
   */
  ALLOCA,
  
  /*
   * Allocates an object and pushes the address to the operand stack
   * 
   * This instruction requires a numerical value indicating whether the object is sealed
   * 
   * If index != 0, object is sealed 
   * Else, object is unsealed 
   * 
   * TOP -> argVector
   *      |
   * BOTTOM
   */
  ALLOCO,
  
  /*
   * Adds a value to the end of an array
   * 
   * TOP    -> targetArray
   *         | value
   * BOTTOM ->
   * 
   * This instruction re-pops the targetArray after execution
   */
  @Deprecated
  LADD,
  
  /*
   * Indexes an object with another object and loads the resulting value
   * to the operand stack. This instruction
   * is meant for arrays and dictionaries.
   * 
   * Syntax: target[index]
   * 
   * STACK Structure:
   * 
   * TOP    ->  target
   *         |  index
   * BOTTOM ->
   */
  LOADIN,
  
  /*
   * Indexes an object with another object and stores a object at that index.
   * 
   * Syntax: target[index] = value
   * 
   * STACK Structure:
   * 
   * TOP    ->  target
   *         |  index
   *         |  value
   * BOTTOM ->
   */
  STOREIN,
  
  /*
   * Makes an empty argVector and pushes it on the operand stack
   */
  MAKEARGV,
  
  /*
   * Treats the value on top of the operand stack as a function argument.
   * 
   * This opcode takes an index (from the constant pool), that's points
   * to the string keyword of this argument.
   * 
   * If the argument is not a keyword argument, then the passed argument should be a negative integer.
   * 
   * This opcode is helpful in efficient function calling
   * 
   * STACK Structure:
   * 
   * TOP    -> value 
   *         | argVector
   * BOTTOM ->
   * 
   * This instruction repops the argVector on the operand stack
   */
  ARG,
  
  /*
   * Dummy instruction. Does absolutely nothing
   */
  PASS;
  
  private static final Set<OpCode> ARG_INSTRS;  //Instructions that require an argument
  
  static {
    OpCode [] options = {STORE, LOAD, STOREATTR, LOADATTR, 
                         LOADC, JUMP, COMMENT, LABEL, 
                         LOADMV, STOREMV, LOAD_CL, STORE_CL, 
                         ARG, LOADMOD,
                         CONSTMV, EXPORTMV};
    
    ARG_INSTRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(options)));
  }
    
  static boolean isArithmetic(OpCode opCode) {
    return opCode.ordinal() >= ADD.ordinal() && opCode.ordinal() <= NEG.ordinal();
  }
  
  static boolean isBitwiseOperator(OpCode opCode) {
    return opCode.ordinal() >= BAND.ordinal() && opCode.ordinal() <= BOR.ordinal();
  }
  
  static boolean isJumpInstr(OpCode opCode) {
    return opCode.ordinal() >= JUMP.ordinal() && opCode.ordinal() <= RET.ordinal();
  }
  
  static boolean isANoArgInstr(OpCode opCode) {
    return !ARG_INSTRS.contains(opCode);
  }
}
