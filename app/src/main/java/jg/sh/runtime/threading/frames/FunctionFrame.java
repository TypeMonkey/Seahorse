package jg.sh.runtime.threading.frames;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.instrs.StoreInstr;
import jg.sh.parsing.token.TokenType;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.loading.ContextualInstr;
import jg.sh.runtime.loading.IndexedJumpInstr;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeDataRecord;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.RuntimeInstance.AttrModifier;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.literals.FuncOperatorCoupling;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.util.RuntimeUtils;

/**
 * Represents a StackFrame for a RuntimeCallable.
 * 
 * This stack frame tracks the current instruction index in the execution
 * of a RuntimeCallable.
 * 
 * @author Jose
 *
 */
public class FunctionFrame extends StackFrame {

  private static Logger LOG = LogManager.getLogger(FunctionFrame.class);

  static volatile int frameMarker = 0;

  private final RuntimeCallable callable;
  private final ContextualInstr [] instrs;

  private int instrIndex;

  /**
   * This is solely used by the CALL instruction 
   * when being invoked on a RuntimeDataRecord.
   * 
   * Since a CALL instruction immediately returns the new frame
   * of the constructor, there's no chance to "overwrite" whatever return
   * value of that constructor with the allocated instance.
   * 
   * With this place holder variable, we can signal to the next invocation of
   * run() that we need to push the value of "passOver" on the operand stack first.
   * 
   */
  private RuntimeInstance passOver;
  
  public FunctionFrame(RuntimeModule hostModule, 
                       RuntimeCallable callable, 
                       int instrIndex, 
                       ArgVector initialArgs) {
    super(hostModule, initialArgs);
    this.callable = callable;
    this.instrs = callable.getCodeObject().getInstrs();
    this.instrIndex = instrIndex;
  }  

  @Override
  public StackFrame run(HeapAllocator allocator, Fiber thread) {

    /**
     * Use solely by CALL when doing data definition instantiation.
     * See comments for passOver
     */
    if (passOver != null) {
      pushOperand(passOver);
      passOver = null;
    }

    /*
    System.out.println(" ====> returning: "+getInstrIndex()+" | "+
                        instrs.length+" | "+
                        instrs[instrs.length - 1].getInstr()+" | "+
                        hasError()+" | "+
                        instrs[instrs.length - 1].getExceptionJumpIndex());
    */

    if (getError() != null) {
      if (hasInstrLeft() && getCurrInstr().getExceptionJumpIndex() >= 0) {
        setInstrIndex(getCurrInstr().getExceptionJumpIndex());
      }
      else {
        return null;
      }
    }
    
    while (hasInstrLeft()) {
      
      final ContextualInstr current = getCurrInstr();
      final Instruction instr = current.getInstr();
      final OpCode op = instr.getOpCode();
      
      System.out.println(instr+" | "+instr.getStart());
      
      switch (op) {
        //Ineffectual instructions. They just fall through
        case LABEL:
        case PASS:
        case COMMENT:
          break;  
        case NOTEQUAL:
        case EQUAL: {
          /**
           * EQUAL is a short circuit of:
           * 
           * left == right || left.$equals(right)
           * 
           * But we only perform the right clause if
           * the left instance has an "equals" method
           */

          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();

          if (left == right) {
            pushOperand(allocator.allocateBool(true));
            break;
          }

          final RuntimeInstance result = RuntimeUtils.numEqual(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
          
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
          
          if (left.hasAttr(opFuncName)) {
            RuntimeInstance func = left.getAttr(opFuncName);
            if (func instanceof Callable) {
              Callable actualCallable = (Callable) func;     
              ArgVector args = new ArgVector(right);
              
              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (CallSiteException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                returnError(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  returnError(error); 
                  return null;
                }
              }
            }
            else {
              pushOperand(allocator.allocateBool(false));
            }
          }
          else {
            pushOperand(allocator.allocateBool(false));
          }
                    
          break;
        }
          
        //Arithmetic instruction opcodes
        case ADD: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();

          /*
           * Check if we're ADDing two objects - one operand being a String.
           * If so, add both instance's toString() result
           */
          if (left instanceof RuntimeString || right instanceof RuntimeString) {
            pushOperand(allocator.allocateString(left.toString() + right.toString()));
            break;
          }
          
          final RuntimeInstance result = RuntimeUtils.numAdd(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case SUB: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeInstance result = RuntimeUtils.numMinus(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case MUL: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeInstance result = RuntimeUtils.numMult(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case DIV: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeInstance result = RuntimeUtils.numDiv(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        
        //Bitwise operator
        case BAND: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
            RuntimeInteger lefInt = (RuntimeInteger) left;
            RuntimeInteger rightInt = (RuntimeInteger) right;
            pushOperand(allocator.allocateInt(lefInt.getValue() & rightInt.getValue()));
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case BOR: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
            RuntimeInteger lefInt = (RuntimeInteger) left;
            RuntimeInteger rightInt = (RuntimeInteger) right;
            pushOperand(allocator.allocateInt(lefInt.getValue() | rightInt.getValue()));
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
          
        //Comparative operators
        case LESS: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeBool result = RuntimeUtils.numLess(left, right, false, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case GREAT: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeBool result = RuntimeUtils.numGreat(left, right, false, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case LESSE: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeBool result = RuntimeUtils.numLess(left, right, true, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }
        case GREATE: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeBool result = RuntimeUtils.numGreat(left, right, true, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
        }         
        case MOD: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();
          
          final RuntimeInstance result = RuntimeUtils.numMod(left, right, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the numXXX() methods, then pass it down!
          
          LOG.trace(" For: "+instr+" => Not numerical operands. Will call overloaded operator implementation!");

          //System.out.println(" --- arith instr! "+op+" | "+right.getClass()+" | "+left.getClass());
                  
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
                  
          if (left.hasAttr(opFuncName)) {
            RuntimeInstance func = left.getAttr(opFuncName);
            if (func instanceof Callable) {           
              Callable actualCallable = (Callable) func;          
              ArgVector args = new ArgVector(right);    

              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();     
                
                //System.out.println("---- returning new frame!!! "+getCurrInstr().getInstr()+" | "+instrIndex);
                return newFrame;
              } catch (CallSiteException e) {
                //System.out.println("--- caught error!!! "+e.getClass()+" | "+e.getMessage());
                RuntimeError error = allocator.allocateError(e.getMessage());
                returnError(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  returnError(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError(coupling.getOpCode().name().toLowerCase()+" isn't a callable");
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error); 
                return null;
              }
            }
          }
          else {
            //unsupported operation
            RuntimeError error = allocator.allocateError("Unsupported operation for "+
                                                         coupling.getOpCode().name().toLowerCase()+
                                                         " on "+left.getClass());
            
            //System.out.println("---- err: "+instr+" | "+error.getAttr("msg")+" | "+(current.getExceptionJumpIndex() >= 0)+" | "+left);
            //System.out.println(instr.getStart());

            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          
          break;
        }
        
        //unary operator
        case NOT: {
          RuntimeInstance operand = popOperand();

          final RuntimeInstance result = RuntimeUtils.negate(operand, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the negate(), then pass it down!

        }
        case NEG: {
          RuntimeInstance operand = popOperand();

          final RuntimeInstance result = RuntimeUtils.negative(operand, allocator);
          if (result != null) {
            pushOperand(result);
            break;
          }

          //If the result is still null after using the negate(), then pass it down!
          
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
          
          if (operand.hasAttr(opFuncName)) {
            RuntimeInstance func = operand.getAttr(opFuncName);

            if (func instanceof Callable) {         
              Callable actualCallable = (Callable) func;
              ArgVector args = new ArgVector();
              
              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (CallSiteException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                returnError(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  returnError(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError("Object isn't callable!");
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error); 
                return null;
              }
            }
          }
          else {
            //unsupported operation          
            RuntimeError error = allocator.allocateError("Unsupported operation for "+coupling.getOpCode().name().toLowerCase());
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          
          break;
        }            
        case CALL: {          
          
          /*
          * Argument structure (from IRCompiler.java)
          * 
          * The first local variable - at index 0 - is the function itself
          * --------------------
          * The second local variable - at index 1 - is the self object.
          * 
          * For module functions, the self object is simply the module object
          * 
          * For class functions/methods, the self object is the instance of the class on which the method is being invoked on
          * 
          * For anonymous functions, it varies:
          *  -> if an anonymous function is defined within an object literal, self is the nearest object they're in
          *  -> else, the anonymous function inherits the self of their host function
          */
          
          final RuntimeInstance callable = popOperand();
          final ArgVector args = (ArgVector) popOperand();
          
          if (callable instanceof Callable) {
            Callable actualCallable = (Callable) callable;
            
            try {
              final RuntimeInstance result = RuntimeUtils.fastCall(actualCallable, args, thread);
              //System.out.println(" =================== CALL =================== ");
              if (result != null) {
                pushOperand(result);
              }
              else {
                //System.out.println(" ===>pre args: "+args.getPositionals().size());
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              }
            } catch (CallSiteException | InvocationException e) {
              LOG.debug(e);
              RuntimeError error = allocator.allocateError(e.getMessage());
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error);
                return null;
              }
            }          
          }
          else if(callable instanceof RuntimeDataRecord) {
            final RuntimeDataRecord dataRecord = (RuntimeDataRecord) callable;
            LOG.info(" ===> call to data record!!!");

            /*
            *TODO: Should this cast be more explicitly checked? Do we want to do a sanity check 
            *      if a "constr" attr exists? and if it does, whether it's a CodeObject?
            */
            final RuntimeCodeObject constructor = (RuntimeCodeObject) dataRecord.getAttr(TokenType.CONSTR.name().toLowerCase());
            final RuntimeInstance selfObject = allocator.allocateEmptyObject();
            final RuntimeCallable actualCallable = allocator.allocateCallable(getHostModule(), selfObject, constructor);

            try {
              final RuntimeInstance result = RuntimeUtils.fastCall(actualCallable, args, thread);
              if (result != null) {
                pushOperand(result);
              }
              else {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                passOver = selfObject;
                incrmntInstrIndex();
                return newFrame;
              }
            } catch (CallSiteException | InvocationException e) {
              RuntimeError error = allocator.allocateError(e.getMessage());
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error);
                return null;
              }
            }
          }
          else {
            //unsupported operation
            RuntimeError error = allocator.allocateError("Target isn't callable "+callable);
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error);
              return null;
            }
          }
          
          break;
        }
        
        /*
        * Jump opcodes
        */
        case JUMP: {
          IndexedJumpInstr jumpInstr = (IndexedJumpInstr) instr;
          setInstrIndex(jumpInstr.getJumpIndex());
          decrmntInstrIndex();
          break;
        }
        case JUMPT: {
          IndexedJumpInstr jumpInstr = (IndexedJumpInstr) instr;

          RuntimeInstance boolValue = popOperand();
          
          /*
          * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
          * 
          * At the moment, we're using strict typing by checking if the condition value
          * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
          * 
          * Should we do the same?
          */
          if ((boolValue instanceof RuntimeBool) && ((RuntimeBool) boolValue).getValue()) {
            setInstrIndex(jumpInstr.getJumpIndex());
            decrmntInstrIndex();
          }
          break;
        }
        case JUMPF: {
          IndexedJumpInstr jumpInstr = (IndexedJumpInstr) instr;

          RuntimeInstance boolValue = popOperand();
          
          /*
          * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
          * 
          * At the moment, we're using strict typing by checking if the condition value
          * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
          * 
          * Should we do the same?
          */
          if ((boolValue instanceof RuntimeBool) && !((RuntimeBool) boolValue).getValue()) {
            setInstrIndex(jumpInstr.getJumpIndex());
            decrmntInstrIndex();
          }
          break;
        }
  
        /*
        * Return opcodes
        */
        case RET : {
          //breaks the instruction execution loop
          
          /*
          RuntimeInstance returnValue = popOperand();
          popFrame();
          
          
          if(hasFrame()) {
            pushOperand(returnValue);
          }
          else {
            this.leftOverReturn = returnValue;
          }
          */
          //System.out.println(" ========================== RETURN ========================");
          returnValue(popOperand());
          return null;
        }
        case RETE: {
          RuntimeInstance potentialException = popOperand();
          
          RuntimeError error = null;
          if(potentialException instanceof RuntimeError) {
            error = (RuntimeError) potentialException;
          }
          else {
            
            /*
            * TODO: For a dynamic language, not quite sure if it's more convenient to allow
            *       for the programmer to throw any object, or error objects specifically.
            *       
            * Cases to consider:
            * - No inheritance in SeaHorse, so it doesn't make sense to check if an object is a "child" of 
            *   an error type
            * - Everything is an object in SeaHorse. What makes an "Error"/"Exception" object any different 
            *   from a "RuntimeObject"? Perhaps the recording of stack traces? But that can be easily added on to a 
            *   plain object in SH....
            * - Allow a user to throw anything, but wrap it in an internal Error object with stack tracing...
            *   We already kinda do this with RuntimeError...
            *   
            * Python disallows the throwing of non-Exception instances
            * JavaScript allows the throwing of any object. No wrapping is done to the thrown object
            */
            
            error = allocator.allocateError("Thrown Exception");
            try {
              error.setAttribute("value", potentialException);
            } catch (OperationException e) {
              //Should never happen as we just allocated the error object. Panic!
              throw new Error(e);
            }
          }
          
          if (current.getExceptionJumpIndex() >= 0) {
            returnError(error);
            setInstrIndex(current.getExceptionJumpIndex());
          }
          else {
            returnError(error);
            return null;
          }
          //break execLoop;
        }
        case POPERR: {
          pushOperand(getError().getErrorObject());
          returnError(null);
        }     
        case MAKEARGV : {
          pushOperand(new ArgVector());
          break;
        }
        case ARG: {
          LoadInstr argInstr = (LoadInstr) instr;          
          //Pop the actual argument
          final RuntimeInstance argValue = popOperand();
          
          ArgVector argVector = (ArgVector) popOperand();

          //System.out.println(" ===> arg instr!");
          
          if (argInstr.getIndex() >= 0) {
            String argName = ((RuntimeString) getHostModule().getConstantMap().get(argInstr.getIndex())).getValue();
            argVector.setKeywordArg(argName, argValue);

            //System.out.println(" ====> Setting arg keyword "+argName+" | value = "+argValue);
          }
          else {
            argVector.addPositional(argValue);
          }
          
          pushOperand(argVector);
          
          break;
        }
        
        /*
        * Load/store instructions 
        */
        case LOADC: {
          LoadInstr loadcInstr = (LoadInstr) instr;
          RuntimeInstance constant = getHostModule().getConstantMap().get(loadcInstr.getIndex());
          pushOperand(constant);
          LOG.info(" ==> LOADC "+loadcInstr.getIndex()+" || "+constant);
          break;
        }
        case LOAD: {          
          LoadInstr loadInstr = (LoadInstr) instr;
          
          //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
          pushOperand(getLocalVar(loadInstr.getIndex()));
          break;
        }
        case STORE: {
          StoreInstr storeInstr = (StoreInstr) instr;
          RuntimeInstance value = popOperand();
          storeLocalVar(storeInstr.getIndex(), value);
          //System.out.println(" ==== STORE: "+instr+" | "+instr.getStart()+" | AT: "+hashCode());
          break;
        }
        case LOADATTR: {
          LoadInstr loadInstr = (LoadInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(loadInstr.getIndex())).getValue();
          RuntimeInstance object = popOperand();
          
          //System.out.println("====> object attr: "+object.attrs());

          if(object.hasAttr(attrName)) {
            pushOperand(object.getAttr(attrName));
          }
          else {
            //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet()+" | "+object.getClass()+" | "+instr.getStart());
            
            RuntimeError error = allocator.allocateError("'"+attrName+"' is unfound on object.");
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
            //System.out.println("---------> ATTR ERROR DONE!!! ");
          }
          
          break;
        }
        case STOREATTR: {
          StoreInstr storeInstr = (StoreInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(storeInstr.getIndex())).getValue();
          RuntimeInstance object = popOperand();
          RuntimeInstance value = popOperand();

          //System.out.println(" ===> STORING ATTR: "+attrName+" | "+object.attrModifiers(attrName));
                  
          try {
            object.setAttribute(attrName, value);
            pushOperand(object);
          } catch (OperationException e) {
            RuntimeError error = allocator.allocateError(e.getMessage());
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }

          break;
        }        
        case LOADNULL: {
          pushOperand(RuntimeNull.NULL);
          break;
        }
        case LOAD_CL: {
          LoadInstr loadInstr = (LoadInstr) instr;
          pushOperand(getCapture(loadInstr.getIndex()));
          break;
        }
        case STORE_CL: {
          StoreInstr storeInstr = (StoreInstr) instr;
          RuntimeInstance value = popOperand();
          setCapture(storeInstr.getIndex(), value);
          break;
        }
        case LOADMV: {
          LoadInstr loadInstr = (LoadInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(loadInstr.getIndex())).getValue();
          
          RuntimeInstance moduleObject = getHostModule().getModuleObject();
          
          if(moduleObject.hasAttr(attrName)) {
            pushOperand(moduleObject.getAttr(attrName));
          }
          else {
            //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet());
            
            RuntimeError error = allocator.allocateError("'"+attrName+"' is unfound on module.");
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
            //System.out.println("---------> ATTR ERROR DONE!!! ");
          }
          
          break;
        }
        case STOREMV: {
          StoreInstr storeInstr = (StoreInstr) instr;      
          RuntimeInstance newValue = popOperand();
                    
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(storeInstr.getIndex())).getValue();

          //System.out.println(">>>> STOREMV: "+attrName+" | "+storeInstr.getIndex());

          
          RuntimeInstance moduleObject = getHostModule().getModuleObject();
          
          try {
            moduleObject.setAttribute(attrName, newValue);
          } catch (OperationException e) {
            RuntimeError error = allocator.allocateError(e.getMessage());
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          break;
        }
        case LOADIN: {
          RuntimeInstance index = popOperand();
          RuntimeInstance target = popOperand();
                    
          if (target.hasAttr(RuntimeArray.RETR_INDEX_ATTR)) {
            RuntimeInstance loadIndexFunc = target.getAttr(RuntimeArray.RETR_INDEX_ATTR);
            
            if (loadIndexFunc instanceof Callable) {
              Callable loadIndexCallable = (Callable) loadIndexFunc;
              ArgVector args = new ArgVector(index);
              
              try {
                StackFrame newFrame = makeFrame(loadIndexCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (CallSiteException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                returnError(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  returnError(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError("The target isn't indexible");
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error); 
                return null;
              }
            }
          }
          else {
            RuntimeError error = allocator.allocateError("The target isn't indexible");
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          
          break;
        }
        case STOREIN: {
          RuntimeInstance value = popOperand();
          RuntimeInstance index = popOperand();
          RuntimeInstance target = popOperand();

          
          if (target.hasAttr(RuntimeArray.STORE_INDEX_ATTR)) {
            RuntimeInstance loadIndexFunc = target.getAttr(RuntimeArray.STORE_INDEX_ATTR);
            
            if (loadIndexFunc instanceof Callable) {
              Callable storeIndexCallable = (Callable) loadIndexFunc;
              ArgVector args = new ArgVector(index, value);
              
              try {
                StackFrame newFrame = makeFrame(storeIndexCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (CallSiteException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                returnError(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  returnError(error);
                  return null;
                }
              }      
            }
            else {
              RuntimeError error = allocator.allocateError("The target isn't indexible");
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error); 
                return null;
              }
            }
          }
          else {
            RuntimeError error = allocator.allocateError("The target isn't indexible");
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          
          break;
        }
        case LOADMOD: {
          LoadInstr loadInstr = (LoadInstr) instr;
          
          if(loadInstr.getIndex() < 0) {
            //Load the current module
            pushOperand(getHostModule().getModuleObject());
          }
          else {
            String moduleName = ((RuntimeString) getHostModule().getConstantMap().get(loadInstr.getIndex())).getValue();
            
            
            RuntimeModule module = thread.getFinder().load(moduleName);

            if (module != null) {
              if(!module.isLoaded()) {
                try {
                  Callable moduleCallable = module.getModuleCallable();
                  StackFrame newFrame = makeFrame(moduleCallable, new ArgVector(), allocator);               
                  incrmntInstrIndex();
                  
                  module.setAsLoaded(true);
                  pushOperand(module.getModuleObject());
                  
                  //System.out.println("----- return loaded module code "+" | "+hasOperand()+" | "+hashCode());
                  
                  return newFrame;
                } catch (CallSiteException e) {
                  RuntimeError error = allocator.allocateError(e.getMessage());
                  returnError(error);
                  if (current.getExceptionJumpIndex() >= 0) {
                    setInstrIndex(current.getExceptionJumpIndex());
                  }
                  else {
                    returnError(error); 
                    return null;
                  }
                }
              }
              else {
                pushOperand(module.getModuleObject());
              }
            }
            else {
              //Unfound module. Throw an error
              RuntimeError error = allocator.allocateError("Couldn't find the module '"+moduleName+"'");
              returnError(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                returnError(error); 
                return null;
              }
            }
          }
          
          break;
        }
        
        /*
        * Module variable modifier instructions 
        * 
        * (in the future, these may be extended to object attributes in general)
        */
        case EXPORTMV: {
          ArgInstr exportInstr = (ArgInstr) instr;
          String varName = ((RuntimeString) getHostModule().getConstantMap().get(exportInstr.getArgument())).getValue();

          final RuntimeInstance moduleObject = getHostModule().getModuleObject();

          //System.out.println("===> making module variable "+varName+" export!");

          try {
            moduleObject.appendAttrModifier(varName, AttrModifier.EXPORT);
          } catch (OperationException e) {
            //Should never happen as the IRCompiler should always put a 
            //CONSTMV right after the initial value of a module variable has been set
            throw new Error(e);
          }
          
          break;
        }
        case CONSTMV: {
          ArgInstr exportInstr = (ArgInstr) instr;
          String varName = ((RuntimeString) getHostModule().getConstantMap().get(exportInstr.getArgument())).getValue();

          //System.out.println("===> making module variable "+varName+" constant!");

          final RuntimeInstance moduleObject = getHostModule().getModuleObject();
          try {
            moduleObject.appendAttrModifier(varName, AttrModifier.CONSTANT);
          } catch (OperationException e) {
            //Should never happen as the IRCompiler should always put a 
            //CONSTMV right after the initial value of a module variable has been set
            throw new Error(e);
          }

          break;
        }
        case ALLOCF: {
          RuntimeInstance codeObject = popOperand();
          
          if (codeObject instanceof RuntimeCodeObject) {
            RuntimeCodeObject actualCodeObject = (RuntimeCodeObject) codeObject;
            
            //Capture local variables based on the current frame
            CellReference [] capturedLocals = new CellReference[actualCodeObject.getCaptures().length];
            for(int dest = 0; dest < capturedLocals.length; dest++) {
              capturedLocals[dest] = getCaptureReference(actualCodeObject.getCaptures()[dest]);
            }
            
            //System.out.println(" ---> ALLOCF, CAPTURED LOCAL: "+capturedLocals.length+" "+Arrays.toString(actualCodeObject.getCaptures()));
            
            RuntimeInstance self = popOperand();
            RuntimeCallable callable = allocator.allocateCallable(getHostModule(), self, actualCodeObject, capturedLocals);
            pushOperand(callable);
          }
          else {
            RuntimeError error = allocator.allocateError("Not a code object");
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          
          break;
        }
        /*
        case CAPTURE: {
          CaptureInstr captureInstr = (CaptureInstr) instr;
          if (isInitialized) {
            
          }
          break;
        }
        */
        case ALLOCA: {
          ArgVector args = (ArgVector) popOperand();
          
          RuntimeArray array = allocator.allocateEmptyArray();
          
          for(int i = 0; i < args.getPositionals().size(); i++) {
            System.out.println("=== ADDING: "+args.getPositional(i));
            array.addValue(args.getPositional(i));
          }
          
          pushOperand(array);
          break;
        }
        case ALLOCO: {
          final ArgInstr alloco = (ArgInstr) instr;

          ArgVector args = (ArgVector) popOperand();
          
          RuntimeInstance object = allocator.allocateEmptyObject((ini, self) -> {
            for(Entry<String, RuntimeInstance> pair : args.getAttributes().entrySet()) { 
              if (pair.getValue() instanceof RuntimeCallable) {
                RuntimeCallable callable = (RuntimeCallable) pair.getValue();
                ini.init(pair.getKey(), callable.rebind(self, allocator));
              }
              else {
                ini.init(pair.getKey(), pair.getValue());
              }
            }
          });

          if (alloco.getArgument() != 0) {
            object.seal();
          }

          pushOperand(object);
          break;
        }
        case BIND: {
          Callable func = (Callable) popOperand();
          RuntimeInstance newSelf = popOperand();

          func = func.rebind(newSelf, allocator);
          pushOperand(func);
          break;
        }
        case MAKECONST: {
          final LoadInstr hasInstr = (LoadInstr) instr;

          final RuntimeInstance attrValue = popOperand();
          final RuntimeInstance targetObj = popOperand();
          final String attrName = ((RuntimeString) getHostModule().getConstantMap().get(hasInstr.getIndex())).getValue();

          try {
            targetObj.setAttribute(attrName, attrValue);
            targetObj.appendAttrModifier(attrName, AttrModifier.CONSTANT);
            pushOperand(targetObj);
          } catch (OperationException e) {
            RuntimeError error = allocator.allocateError("Cannot make '"+attrName+"' constant: "+e.getMessage());
            returnError(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              returnError(error); 
              return null;
            }
          }
          break;
        }
        case SEAL: {
          final RuntimeInstance obj = popOperand();
          obj.seal();
          pushOperand(obj);
          break;
        }
        case HAS_KARG: {
          final LoadInstr hasInstr = (LoadInstr) instr;
          final String attrName = ((RuntimeString) getHostModule().getConstantMap().get(hasInstr.getIndex())).getValue();
          final RuntimeBool result = allocator.allocateBool(initialArgs.hasAttr(attrName));
          //System.out.println(" ===> has k_arg? "+attrName+" | "+initialArgs.attrs()+" | "+result);
          pushOperand(result);
          break;
        }
        case CALLA:
        case CAPTURE:
        case LADD:
        case LOADSELF:
          LOG.warn("Deprecated opcode: "+instr+" >>>>>>>>>>>>>>>");
          break;
        default:
          LOG.warn("Unknown instruction: "+instr+" >>>>>>>>>>>>>>>>>");
      }
      
      incrmntInstrIndex();
    }
        
    return null;
  }

  public void setInstrIndex(int newIndex) {
    this.instrIndex = newIndex;
  }
  
  public void incrmntInstrIndex() {
    setInstrIndex(instrIndex + 1);
  }
  
  public void decrmntInstrIndex() {
    setInstrIndex(instrIndex - 1);
  }
  
  @Override
  protected void markAdditional(Cleaner allocator) {}
  
  public int getInstrIndex() {
    return instrIndex;
  }
  
  public boolean hasInstrLeft() {
    return instrIndex < instrs.length;
  }
  
  public ContextualInstr getCurrInstr() {
    return instrs[instrIndex];
  }
  
  public RuntimeInstance getCapture(int varIndex) {
    return callable.getCapture(varIndex);
    //return closureCaptures[varIndex].getValue();
  }

  public void setCapture(int varIndex, RuntimeInstance value) {
    callable.setCapture(varIndex, value);
    //closureCaptures[varIndex].setValue(value);
  }
  
  public CellReference getCaptureReference(int varIndex) {
    return callable.getCaptures()[varIndex];
  }
  
  @Override
  public RuntimeCallable getCallable() {
    return callable;
  }

}
