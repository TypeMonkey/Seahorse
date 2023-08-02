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
   * 
   * Instruction class: NoArgInstr
   */
  ADD,
  SUB,
  MUL,
  DIV,
  MOD,
  NEG,
  
  /*
   * Comparative operators
   * 
   * Instruction class: NoArgInstr
   */
  LESS,
  GREAT,
  LESSE,
  GREATE,
  EQUAL,  //Object equality
  
  /*
   * Boolean operators.
   * 
   * Both AND and OR are compiled away to their explicit
   * instruction list equivalents
   * 
   * Instruction class: NoArgInstr
   */
  NOT,
  
  /*
   * Bitwise operators 
   * 
   * Instruction class: NoArgInstr
   */
  BAND,
  BOR,

  /*
   * Increments an integer by 1, or a float by 1
   */
  INC,

  /**
   * Decrements an integer by 1, or a float by 1
   */
  DEC,
  
  /*
   * Instruction jumps and function calls
   * 
   * Instruction class: JumpInstr
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
   * If the value on top of the operand stack is a data record, then it this 
   * instruction will intiaite the object instatiation process.
   * 
   * Object instantiation process:
   * 1.) Allocate empty RuntimeObject
   * 2.) Set immutable $type field to be the data record
   * 3.) Call constructor with the RuntimeObject
   * 4.) Seal object if data record is sealed
   * 
   * The return value returned by the function will be popped to the top of the operand stack
   * 
   * TOP    -> callable or dataRecord
   *         | argVector  <- should include "self"
   * BOTTOM ->
   * 
   */
  CALL,  //Will treat the object on top of the operand stack as a function 
  
  
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
   * Makes an attribute on an object immutable.
   * 
   * This instruction requires an index which corresponds to the string attribute name on the constant pool
   * 
   * TOP    -> initialValue
   *         | target
   * BOTTOM ->
   * 
   * This instruction re-pops the value of the target object's attribute value.
   * 
   * Note: the attribute must be a new addition to the object. If this
   *       instruction is used on an existing attribute, an error will be thrown.
   * 
   * Instruction class: LoadInstr (due to its reliance on the constant pool)
   */
  MAKECONST,
  
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

  /**
   * Reallocates a new function (based on the given function's code object)
   * and binds it to the given object.
   * 
   * TOP      -> function
   *           | self
   * BOTTOM   ->
   * 
   * This instruction pops back the function
   */
  BIND,
  
  /*
   * Creates an instance of a function from a code object at the top of the operand stack
   * 
   * TOP      -> codeObject
   *           | self
   * BOTTOM   ->
   * 
   * This instruction then pops the allocated callable function on the top of the operand stack
   */
  ALLOCF,
  
  
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

  /**
   * Checks if an ArgVector has a given optional/keyword argument
   * 
   * This opcode requires an index correlating to the keyword (as a string)
   * to check for.
   * 
   * TOP    -> ArgVector
   *         |
   * BOTTOM ->
   * 
   * At the completion of this instruction, the ArgVector is popped back into the operand stack
   * , and then the boolean value (true: ArgVector has been provided with the optional argument, false if else.)
   * 
   * So, the result state should be:
   * 
   * TOP     -> true or false
   *          | ArgVector
   * BOTTOM  -<
   * 
   * Instruction class: LoadInstr (due to its reliance on the constant pool)
   */
  HAS_KARG,
  
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
   * 
   * Instruction class: LoadInstr (due to its reliance on the constant pool)
   */
  ARG,

  /**
   * Seals an object from further attribute additions, removals and mutations.
   * 
   * TOP     -> object
   *          |
   * BOTTOM  ->
   */
  SEAL,
  
  /*
   * Dummy instruction. Does absolutely nothing
   */
  PASS;
  
  private static final Set<OpCode> ARG_INSTRS;  //Instructions that require an argument (Includes non-ArgInstr OpCodes, like JumpInstrs)
  private static final Set<OpCode> POOL_INSTRS; //Instructions whose arguments rely on the constant pool
  
  static {
    OpCode [] options = {STORE, LOAD, STOREATTR, LOADATTR, 
                         LOADC, JUMP, JUMPF, JUMPT, COMMENT, LABEL, 
                         LOADMV, STOREMV, LOAD_CL, STORE_CL, 
                         ARG, ALLOCO, LOADMOD, MAKECONST,
                         CONSTMV, EXPORTMV, HAS_KARG};
    
    ARG_INSTRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(options)));

    options = new OpCode[]
                        {STOREATTR, LOADATTR, 
                         LOADC,LOADMV, STOREMV, 
                         ARG, LOADMOD, MAKECONST,
                         CONSTMV, EXPORTMV, HAS_KARG};
    
    POOL_INSTRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(options)));
  }
    
  public static boolean isArithmetic(OpCode opCode) {
    return opCode.ordinal() >= ADD.ordinal() && opCode.ordinal() <= NEG.ordinal();
  }
  
  public static boolean isBitwiseOperator(OpCode opCode) {
    return opCode.ordinal() >= BAND.ordinal() && opCode.ordinal() <= BOR.ordinal();
  }
  
  public static boolean isJumpInstr(OpCode opCode) {
    return opCode.ordinal() >= JUMP.ordinal() && opCode.ordinal() <= RET.ordinal();
  }
  
  public static boolean isANoArgInstr(OpCode opCode) {
    return !ARG_INSTRS.contains(opCode);
  }

  public static boolean reliesOnConstantPool(OpCode opCode) {
    return POOL_INSTRS.contains(opCode);
  }
}
