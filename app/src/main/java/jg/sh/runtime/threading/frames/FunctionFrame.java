package jg.sh.runtime.threading.frames;

import java.util.Map.Entry;
import java.util.function.BiConsumer;

import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.instrs.StoreCellInstr;
import jg.sh.parsing.token.TokenType;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.InvocationException;
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
import jg.sh.runtime.objects.RuntimeObject;
import jg.sh.runtime.objects.RuntimeObject.AttrModifier;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.literals.FuncOperatorCoupling;
import jg.sh.runtime.objects.literals.RuntimeBool;
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

  private int instrIndex;
  
  public FunctionFrame(RuntimeModule hostModule, 
                       RuntimeCallable callable, 
                       int instrIndex, 
                       BiConsumer<RuntimeInstance, Throwable> atCompletion) {
    super(hostModule, callable, atCompletion);
    this.instrIndex = instrIndex;
  }  

  @Override
  public StackFrame run(HeapAllocator allocator, Fiber thread) {
    if (getError() != null) {
      if (getCurrInstr().getExceptionJumpIndex() >= 0) {
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
      
      //System.out.println(instr+" | "+instr.getLine());
      
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
           * left == right || left.equals(right)
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

          /**
           * Check if we're both operands are primitives. If so, we can skip
           * the function call routine and operate directly
           */
          RuntimeInstance fastNumResult = null;
          if ((fastNumResult = RuntimeUtils.fastNumArith(left, right, op, allocator)) != null) {
            pushOperand(fastNumResult);
            break;
          } 
          
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
          
          if (left.hasAttr(opFuncName)) {
            RuntimeInstance func = left.getAttr(opFuncName);
            if (func instanceof Callable) {
              Callable actualCallable = (Callable) func;     
              ArgVector args = new ArgVector(actualCallable, actualCallable.getSelf(), right);
              
              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (InvocationException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                setErrorFlag(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  setErrorFlag(error); 
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
        case ADD: 
        case SUB: 
        case MUL: 
        case DIV: 
        
        //Bitwise operator
        case BAND:
        case BOR:
          
        //Comparative operators
        case LESS: 
        case GREAT:
        case LESSE:
        case GREATE:          
        case MOD: {
          RuntimeInstance right = popOperand();
          RuntimeInstance left = popOperand();

          /**
           * Check if we're both operands are primitives. If so, we can skip
           * the function call routine and operate directly
           */
          RuntimeInstance fastNumResult = null;
          if ((fastNumResult = RuntimeUtils.fastNumArith(left, right, op, allocator)) != null) {
            pushOperand(fastNumResult);
            break;
          } 
          

          //System.out.println(" --- arith instr! "+op+" | "+right.getClass()+" | "+left.getClass());
                  
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
                  
          if (left.hasAttr(opFuncName)) {
            RuntimeInstance func = left.getAttr(opFuncName);
            if (func instanceof Callable) {           
              Callable actualCallable = (Callable) func;          
              ArgVector args = new ArgVector(actualCallable, actualCallable.getSelf(), right);    

              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();     
                
                //System.out.println("---- returning new frame!!!");
                return newFrame;
              } catch (InvocationException e) {
                //System.out.println("--- caught error!!! "+e.getClass()+" | "+e.getMessage());
                RuntimeError error = allocator.allocateError(e.getMessage());
                setErrorFlag(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  setErrorFlag(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError(coupling.getOpCode().name().toLowerCase()+" isn't a callable");
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error); 
                return null;
              }
            }
          }
          else {
            //unsupported operation
            RuntimeError error = allocator.allocateError("Unsupported operation for "+coupling.getOpCode().name().toLowerCase());
            
            System.out.println("---- err: "+instr+" | "+error.getAttr("msg")+" | "+(current.getExceptionJumpIndex() >= 0)+" | "+left);
            
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
              return null;
            }
          }
          
          break;
        }
        
        //unary operator
        case NOT:
        case NEG: {
          RuntimeInstance operand = popOperand();
          
          FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(op);
          final String opFuncName = coupling.getFuncName();
          
          if (operand.hasAttr(opFuncName)) {
            RuntimeInstance func = operand.getAttr(opFuncName);

            if (func instanceof Callable) {         
              Callable actualCallable = (Callable) func;
              ArgVector args = new ArgVector(actualCallable, actualCallable.getSelf());
              
              try {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (InvocationException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                setErrorFlag(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  setErrorFlag(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError("Object isn't callable!");
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error); 
                return null;
              }
            }
          }
          else {
            //unsupported operation          
            RuntimeError error = allocator.allocateError("Unsupported operation for "+coupling.getOpCode().name().toLowerCase());
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
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
            args.addAtFront(actualCallable.getSelf());
            args.addAtFront(actualCallable);
            
            try {
              final RuntimeInstance result = RuntimeUtils.fastCall(actualCallable, args, thread);
              if (result != null) {
                pushOperand(result);
              }
              else {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator);
                /**
                 * We push the argvector back on the operand stack
                 * so the function we called can use it for any HASKARG
                 * instructions for parameter checking
                 */
                pushOperand(args);
                incrmntInstrIndex();
                return newFrame;
              }
            } catch (InvocationException e) {
              RuntimeError error = allocator.allocateError(e.getMessage());
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error);
                return null;
              }
            }          
          }
          else if(callable instanceof RuntimeDataRecord) {
            final RuntimeDataRecord dataRecord = (RuntimeDataRecord) callable;

            /*
            *TODO: Should this cast be more explicitly checked? Do we want to do a sanity check 
            *      if a "constr" attr exists? and if it does, whether it's a CodeObject?
            */
            final RuntimeCodeObject constructor = (RuntimeCodeObject) dataRecord.getAttr(TokenType.CONSTR.name().toLowerCase());
            final RuntimeObject selfObject = allocator.allocateEmptyObject();
            final RuntimeCallable actualCallable = allocator.allocateCallable(getHostModule(), selfObject, constructor);
            args.addAtFront(actualCallable.getSelf());
            args.addAtFront(actualCallable);

            try {
              final RuntimeInstance result = RuntimeUtils.fastCall(actualCallable, args, thread);
              if (result != null) {
                pushOperand(result);
              }
              else {
                StackFrame newFrame = makeFrame(actualCallable, args, allocator, (r, e) -> {
                  pushOperand(selfObject);
                });
                incrmntInstrIndex();
                return newFrame;
              }
            } catch (InvocationException e) {
              RuntimeError error = allocator.allocateError(e.getMessage());
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error);
                return null;
              }
            }
          }
          else {
            //unsupported operation
            RuntimeError error = allocator.allocateError("Target isn't callable "+callable);
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error);
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
            error.setAttribute("value", potentialException);
          }
          
          if (current.getExceptionJumpIndex() >= 0) {
            setErrorFlag(error);
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
          setErrorFlag(null);
        }     
        case MAKEARGV : {
          pushOperand(new ArgVector());
          break;
        }
        case ARG: {
          ArgInstr argInstr = (ArgInstr) instr;          
          //Pop the actual argument
          final RuntimeInstance argValue = popOperand();
          
          ArgVector argVector = (ArgVector) popOperand();
          
          if (argInstr.getArgument() >= 0) {
            String argName = ((RuntimeString) getHostModule().getConstantMap().get(argInstr.getArgument())).getValue();
            argVector.setAttribute(argName, argValue);
          }
          else {
            argVector.addAtFront(argValue);
          }
          
          pushOperand(argVector);
          
          break;
        }
        
        /*
        * Load/store instructions 
        */
        case LOADC: {
          ArgInstr loadcInstr = (ArgInstr) instr;
          pushOperand(getHostModule().getConstantMap().get(loadcInstr.getArgument()));
          break;
        }
        case LOAD: {          
          LoadCellInstr loadInstr = (LoadCellInstr) instr;
          
          //System.out.println(" ------- LOAD: LOCAL VARS: "+peekFrame().getLocalVars().length+", "+loadInstr.getIndex());
          
          pushOperand(getLocalVar(loadInstr.getIndex()));
          break;
        }
        case STORE: {
          StoreCellInstr storeInstr = (StoreCellInstr) instr;
          RuntimeInstance value = popOperand();
          storeLocalVar(storeInstr.getIndex(), value);
          break;
        }
        case LOADATTR: {
          LoadCellInstr loadInstr = (LoadCellInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(loadInstr.getIndex())).getValue();
          RuntimeInstance object = popOperand();
          
          if(object.hasAttr(attrName)) {
            pushOperand(object.getAttr(attrName));
          }
          else {
            System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet()+" | "+object.getClass()+" | "+instr.getStart());
            
            RuntimeError error = allocator.allocateError("'"+attrName+"' is unfound on object.");
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
              return null;
            }
            //System.out.println("---------> ATTR ERROR DONE!!! ");
          }
          
          break;
        }
        case STOREATTR: {
          StoreCellInstr storeInstr = (StoreCellInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(storeInstr.getIndex())).getValue();
          RuntimeInstance object = popOperand();
          RuntimeInstance value = popOperand();
                  
          object.setAttribute(attrName, value);
          
          pushOperand(object);
          break;
        }        
        case LOADNULL: {
          pushOperand(RuntimeNull.NULL);
          break;
        }
        case LOAD_CL: {
          LoadCellInstr loadInstr = (LoadCellInstr) instr;
          pushOperand(getCapture(loadInstr.getIndex()));
          break;
        }
        case STORE_CL: {
          StoreCellInstr storeInstr = (StoreCellInstr) instr;
          RuntimeInstance value = popOperand();
          setCapture(storeInstr.getIndex(), value);
          break;
        }
        case LOADMV: {
          LoadCellInstr loadInstr = (LoadCellInstr) instr;
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(loadInstr.getIndex())).getValue();
          
          RuntimeObject moduleObject = getHostModule().getModuleObject();
          
          if(moduleObject.hasAttr(attrName)) {
            pushOperand(moduleObject.getAttr(attrName));
          }
          else {
            //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet());
            
            RuntimeError error = allocator.allocateError("'"+attrName+"' is unfound on module.");
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
              return null;
            }
            //System.out.println("---------> ATTR ERROR DONE!!! ");
          }
          
          break;
        }
        case STOREMV: {
          StoreCellInstr storeInstr = (StoreCellInstr) instr;      
          RuntimeInstance newValue = popOperand();
          
          //System.out.println(">>>> STOREMV: "+loadConstant(storeInstr.getIndex())+" | "+storeInstr.getIndex());
          
          String attrName = ((RuntimeString) getHostModule().getConstantMap().get(storeInstr.getIndex())).getValue();
          
          RuntimeObject moduleObject = getHostModule().getModuleObject();
          
          moduleObject.setAttribute(attrName, newValue);
          
          /*
          if(moduleObject.hasAttr(attrName)) {
            moduleObject.setAttribute(attrName, newValue);
          }
          else {
            //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet());
            
            RuntimeError error = allocator.allocateError("'"+attrName+"' is unfound on module.");
            
            System.out.println(error.getAttr("msg"));
            
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              getPrevFrame().setErrorFlag(error);
              return getPrevFrame();
            }
            //System.out.println("---------> ATTR ERROR DONE!!! ");
          }
          */
          break;
        }
        case LOADIN: {
          RuntimeInstance index = popOperand();
          RuntimeInstance target = popOperand();
                    
          if (target.hasAttr(RuntimeArray.RETR_INDEX_ATTR)) {
            RuntimeInstance loadIndexFunc = target.getAttr(RuntimeArray.RETR_INDEX_ATTR);
            
            if (loadIndexFunc instanceof Callable) {
              Callable loadIndexCallable = (Callable) loadIndexFunc;
              ArgVector args = new ArgVector(loadIndexCallable, target, index);
              
              try {
                StackFrame newFrame = makeFrame(loadIndexCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (InvocationException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                setErrorFlag(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  setErrorFlag(error); 
                  return null;
                }
              }
            }
            else {
              RuntimeError error = allocator.allocateError("The target isn't indexible");
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error); 
                return null;
              }
            }
          }
          else {
            RuntimeError error = allocator.allocateError("The target isn't indexible");
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
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
              ArgVector args = new ArgVector(storeIndexCallable, target, index, value);
              
              try {
                StackFrame newFrame = makeFrame(storeIndexCallable, args, allocator);
                incrmntInstrIndex();
                return newFrame;
              } catch (InvocationException e) {
                RuntimeError error = allocator.allocateError(e.getMessage());
                setErrorFlag(error);
                if (current.getExceptionJumpIndex() >= 0) {
                  setInstrIndex(current.getExceptionJumpIndex());
                }
                else {
                  setErrorFlag(error);
                  return null;
                }
              }      
            }
            else {
              RuntimeError error = allocator.allocateError("The target isn't indexible");
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error); 
                return null;
              }
            }
          }
          else {
            RuntimeError error = allocator.allocateError("The target isn't indexible");
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
              return null;
            }
          }
          
          break;
        }
        case LOADMOD: {
          LoadCellInstr loadInstr = (LoadCellInstr) instr;
          
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
                  StackFrame newFrame = makeFrame(moduleCallable, new ArgVector(moduleCallable, moduleCallable.getSelf()), allocator);               
                  incrmntInstrIndex();
                  
                  module.setAsLoaded(true);
                  pushOperand(module.getModuleObject());
                  
                  //System.out.println("----- return loaded module code "+" | "+hasOperand()+" | "+hashCode());
                  
                  return newFrame;
                } catch (InvocationException e) {
                  RuntimeError error = allocator.allocateError(e.getMessage());
                  setErrorFlag(error);
                  if (current.getExceptionJumpIndex() >= 0) {
                    setInstrIndex(current.getExceptionJumpIndex());
                  }
                  else {
                    setErrorFlag(error); 
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
              setErrorFlag(error);
              if (current.getExceptionJumpIndex() >= 0) {
                setInstrIndex(current.getExceptionJumpIndex());
              }
              else {
                setErrorFlag(error); 
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
          /*
          ArgInstr exportInstr = (ArgInstr) instr;
          String varName = ((RuntimeString) getHostModule().getConstantMap().get(exportInstr.getArgument())).getValue();
          getHostModule().getModuleObject().setAttrModifiers(varName, AttrModifier.EXPORT);
          */
          break;
        }
        case CONSTMV: {
          ArgInstr exportInstr = (ArgInstr) instr;
          String varName = ((RuntimeString) getHostModule().getConstantMap().get(exportInstr.getArgument())).getValue();
          getHostModule().getModuleObject().setAttrModifiers(varName, AttrModifier.CONSTANT);
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
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
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
          
          //We need to add array elements in reverse order from ArgVector
          //As ArgVector adds positional arguments by adding them at the front.
          for(int i = args.getPositionals().size() - 1; i >= 0; i--) {
            array.addValue(args.getPositional(i));
          }
          
          pushOperand(array);
          break;
        }
        case ALLOCO: {
          ArgVector args = (ArgVector) popOperand();
          
          RuntimeObject object = allocator.allocateEmptyObject();
          for(Entry<String, RuntimeInstance> pair : args.getAttributes().entrySet()) {
            
            //System.out.println("---PAIRING: "+pair.getKey()+" , "+pair.getValue().getClass());
            
            if (pair.getValue() instanceof RuntimeCallable) {
              RuntimeCallable callable = (RuntimeCallable) pair.getValue();
              object.setAttribute(pair.getKey(), callable.rebind(object, allocator));
            }
            else {
              object.setAttribute(pair.getKey(), pair.getValue());
            }
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
          final ArgInstr hasInstr = (ArgInstr) instr;

          final RuntimeInstance attrValue = popOperand();
          final RuntimeObject targetObj = (RuntimeObject) popOperand();
          final String attrName = ((RuntimeString) getHostModule().getConstantMap().get(hasInstr.getArgument())).getValue();

          if(!attrValue.hasAttr(attrName)) {
            targetObj.setAttribute(attrName, attrValue);
            targetObj.setAttrModifiers(attrName, AttrModifier.CONSTANT);
          }
          else {
            RuntimeError error = allocator.allocateError(attrName+" is not a new attribute and cannot be made immutable.");
            setErrorFlag(error);
            if (current.getExceptionJumpIndex() >= 0) {
              setInstrIndex(current.getExceptionJumpIndex());
            }
            else {
              setErrorFlag(error); 
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
          final ArgInstr hasInstr = (ArgInstr) instr;

          final ArgVector args = (ArgVector) popOperand();
          final String attrName = ((RuntimeString) getHostModule().getConstantMap().get(hasInstr.getArgument())).getValue();
          pushOperand(args);
          pushOperand(allocator.allocateBool(args.hasAttr(attrName)));
          break;
        }
        case CALLA:
        case CAPTURE:
        case LADD:
        case LOADSELF:
          System.out.println("Deprecated opcode: "+instr+" >>>>>>>>>>>>>>>");
          break;
        default:
          System.err.println("Unknown instruction: "+instr+" >>>>>>>>>>>>>>>>>");
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
    return instrIndex < getRuntimeCallable().getCodeObject().getInstrs().length;
  }
  
  public ContextualInstr getCurrInstr() {
    return getRuntimeCallable().getCodeObject().getInstrs()[instrIndex];
  }
  
  public RuntimeInstance getCapture(int varIndex) {
    return getRuntimeCallable().getCapture(varIndex);
    //return closureCaptures[varIndex].getValue();
  }

  public void setCapture(int varIndex, RuntimeInstance value) {
    getRuntimeCallable().setCapture(varIndex, value);
    //closureCaptures[varIndex].setValue(value);
  }
  
  public CellReference getCaptureReference(int varIndex) {
    return getRuntimeCallable().getCaptures()[varIndex];
  }
  
  /**
   * Convenience method for returning the RuntimeCallable that this FunctionFrame is based on.
   * @return the RuntimeCallable that this FunctionFrame is based on.
   */
  public RuntimeCallable getRuntimeCallable() {
    return (RuntimeCallable) callable;
  }

}
