package jg.sh.runtime.threading.frames;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.compile.instrs.OpCode;
import jg.sh.parsing.token.TokenType;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.Cleaner;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.ModuleLoadException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.instrs.ArgInstruction;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.metrics.GeneralMetrics;
import jg.sh.runtime.metrics.GeneralMetrics.Meaures;
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
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.util.RuntimeUtils;

/**
 * Represents a StackFrame for a RuntimeCallable.
 * 
 * This stack frame tracks the instr instruction index in the execution
 * of a RuntimeCallable.
 * 
 * @author Jose
 *
 */
public class FunctionFrame extends StackFrame {

  private static final Logger LOG = LogManager.getLogger(FunctionFrame.class);

  static volatile int frameMarker = 0;

  private final RuntimeCallable callable;
  private final RuntimeInstruction [] instrs;
  private final RuntimeInstance [] constantMap;
  private final RuntimeModule hostModule;

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
   */
  private RuntimeInstance passOver;
  
  public FunctionFrame(RuntimeModule hostModule, 
                       RuntimeCallable callable, 
                       int instrIndex, 
                       ArgVector initialArgs) {
    super(hostModule, initialArgs);
    this.callable = callable;
    this.instrs = callable.getCodeObject().getInstrs();
    this.hostModule = callable.getHostModule();
    this.constantMap = hostModule.getConstants();
    this.instrIndex = instrIndex;
  }  

  public StackFrame run(HeapAllocator allocator, Fiber thread) {

    /**
     * Use solely by CALL when doing data definition instantiation.
     * See comments for passOver
     */
    if (passOver != null) {
      pushOperand(passOver);
      passOver = null;
    }

    if (getError() != null) {
      if (hasInstrLeft() && getCurrInstr().getExceptionJumpIndex() >= 0) {
        setInstrIndex(getCurrInstr().getExceptionJumpIndex());
      }
      else {
        return null;
      }
    }
    
    while (instrIndex < instrs.length) {
      final RuntimeInstruction instr = instrs[instrIndex];

      final OpCode op = instr.getOpCode();
      
      //System.out.println(instr+" | "+instr.getStart());
      
      StackFrame frame = this;

      final long start = System.nanoTime();
      switch (op) {
        /*
         * Ineffectual instructions. Just skip over them
         */
        case PASS:
        case COMMENT:
        case LABEL:
          break;
        case ADD:
          frame = binAdd(instr, thread, allocator);
          break;
        case ALLOCA:
          frame = allocateArray(instr, thread, allocator);
          break;
        case ALLOCF:
          frame = allocateFunc(instr, thread, allocator);
          break;
        case ALLOCO:
          frame = allocateObject(instr, thread, allocator);
          break;
        case ARG:
          frame = arg(instr, thread, allocator);
          break;
        case BAND:
          frame = binBitwiseAnd(instr, thread, allocator);
          break;
        case BIND:
          frame = bind(instr, thread, allocator);
          break;
        case BOR:
          frame = binBitwiseOr(instr, thread, allocator);
          break;
        case CALL:
          frame = call(instr, thread, allocator);
          break;
        case CONSTMV:
          frame = constantModuleVar(instr, thread, allocator);
          break;
        case DEC:
          frame = dec(instr, thread, allocator);
          break;
        case DIV:
          frame = binDiv(instr, thread, allocator);
          break;
        case EQUAL:
          frame = equality(instr, thread, allocator);
          break;
        case EXPORTMV:
          frame = exportModuleVar(instr, thread, allocator);
          break;
        case GREAT:
          frame = binGreat(instr, thread, allocator);
          break;
        case GREATE:
          frame = binGreatEqual(instr, thread, allocator);
          break;
        case HAS_KARG:
          frame = hasKeywordArg(instr, thread, allocator);
          break;
        case INC:
          frame = inc(instr, thread, allocator);
          break;
        case JUMP:
          frame = jump(instr, thread, allocator);
          break;
        case JUMPF:
          frame = jumpFalse(instr, thread, allocator);
          break;
        case JUMPT:
          frame = jumpTrue(instr, thread, allocator);
          break;
        case LESS:
          frame = binLess(instr, thread, allocator);
          break;
        case LESSE:
          frame = binLessEqual(instr, thread, allocator);
          break;
        case LOAD:
          frame = loadLocal(instr, thread, allocator);
          break;
        case LOADATTR:
          frame = loadAttr(instr, thread, allocator);
          break;
        case LOADC:
          frame = loadConstant(instr, thread, allocator);
          break;
        case LOADIN:
          frame = loadIndex(instr, thread, allocator);
          break;
        case LOADMOD:
          frame = loadModule(instr, thread, allocator);
          break;
        case LOADMV:
          frame = loadModuleVar(instr, thread, allocator);
          break;
        case LOADNULL:
          frame = loadNull(instr, thread, allocator);
          break;
        case LOAD_CL:
          frame = loadCapture(instr, thread, allocator);
          break;
        case MAKEARGV:
          frame = makeArgV(instr, thread, allocator);
          break;
        case MAKECONST:
          frame = makeConstant(instr, thread, allocator);
          break;
        case MOD:
          frame = binMod(instr, thread, allocator);
          break;
        case MUL:
          frame = binMul(instr, thread, allocator);
          break;
        case NEG:
          frame = negative(instr, thread, allocator);
          break;
        case NOT:
          frame = not(instr, thread, allocator);
          break;
        case POPERR:
          frame = popError(instr, thread, allocator);
          break;
        case RET:
          frame = returnFrame(instr, thread, allocator);
          break;
        case RETE:
          frame = throwError(instr, thread, allocator);
          break;
        case SEAL:
          frame = sealObject(instr, thread, allocator);
          break;
        case STORE:
          frame = storeLocal(instr, thread, allocator);
          break;
        case STOREATTR:
          frame = storeAttr(instr, thread, allocator);
          break;
        case STOREIN:
          frame = storeIndex(instr, thread, allocator);
          break;
        case STOREMV:
          frame = storeModuleVar(instr, thread, allocator);
          break;
        case STORE_CL:
          frame = storeCapture(instr, thread, allocator);
          break;
        case SUB:
          frame = binMinus(instr, thread, allocator);
          break;
      }

      final long end = System.nanoTime();
      GeneralMetrics.addTimes(op, end - start);

      if (frame != this) {
        return frame;
      }
      
      instrIndex++;
    }
        
    return null;
  }

  // Bytecode dispatch methods - START

  private StackFrame binAdd(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();

    try {
      final RuntimeInstance result = left.$add(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.ADD), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binMinus(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();

    try {
      final RuntimeInstance result = left.$sub(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.SUB), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binMul(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$mul(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.MUL), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binDiv(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$div(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.DIV), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binBitwiseAnd(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$band(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.BAND), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binBitwiseOr(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$bor(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.BOR), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binLess(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$less(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.LESS), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binGreat(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$great(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.GREAT), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binLessEqual(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$lesse(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.LESSE), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binGreatEqual(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$greate(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.GREATE), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame binMod(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();
    
    try {
      final RuntimeInstance result = left.$mod(right, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.MOD), 
                             instr, 
                             left, 
                             right);
    }
  }

  private StackFrame inc(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance target = popOperand();

    try {
      final RuntimeInstance result = target.$inc(allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.ADD), 
                             instr, 
                             target, 
                             allocator.allocateInt(1));
    }
  }

  private StackFrame negative(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance target = popOperand();
    
    final RuntimeInstance result = RuntimeUtils.negative(target, allocator);
    if (result != null) {
      pushOperand(result);
      return this;
    }

    final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(OpCode.NEG);
    return checkAndCall(allocator, 
                        instr, 
                        target, 
                        coupling.getFuncName(), 
                        new ArgVector(), 
                        "Value for "+coupling.getOpCode().name().toLowerCase()+" isn't callable.", 
                        "Unsupported operator for "+coupling.getOpCode().name().toLowerCase()+" isn't supported for "+target.getClass());
  }

  private StackFrame not(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance target = popOperand();
    
    final RuntimeInstance result = RuntimeUtils.negate(target, allocator);
    if (result != null) {
      pushOperand(result);
      return this;
    }

    final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(OpCode.NOT);
    return checkAndCall(allocator, 
                        instr, 
                        target, 
                        coupling.getFuncName(), 
                        new ArgVector(), 
                        "Value for "+coupling.getOpCode().name().toLowerCase()+" isn't callable.", 
                        "Unsupported operator for "+coupling.getOpCode().name().toLowerCase()+" isn't supported for "+target.getClass());
  }

  private StackFrame dec(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance target = popOperand();
    
    try {
      final RuntimeInstance result = target.$dec(allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return binaryArithCall(allocator, 
                             FuncOperatorCoupling.getCoupling(OpCode.SUB), 
                             instr, 
                             target, 
                             allocator.allocateInt(1));
    }
  }

  private StackFrame binaryArithCall(HeapAllocator allocator, 
                                     FuncOperatorCoupling opFuncName,
                                     RuntimeInstruction instruction, 
                                     RuntimeInstance left, 
                                     RuntimeInstance right) {
    LOG.trace(" For: "+instruction+" => Not numerical operands. Will call overloaded operator implementation!");
    return checkAndCall(allocator, 
                        instruction, 
                        right, 
                        opFuncName.getFuncName(), 
                        new ArgVector(right), 
                        opFuncName.getOpCode().name().toLowerCase()+" isn't a callable", 
                        "Unsupported operation for "+opFuncName.getOpCode().name().toLowerCase()+" on "+left.getClass());
  }

  private StackFrame call(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final RuntimeInstance callable = popOperand();
    final ArgVector args = (ArgVector) popOperand();   
    
    if (callable instanceof Callable) {
      return generalCall(allocator, instr, callable, args, "");
    }
    else if(callable instanceof RuntimeDataRecord) {
      final RuntimeDataRecord dataRecord = (RuntimeDataRecord) callable;
      LOG.info(" ===> call to data record!!!");

      final RuntimeCodeObject constructor = (RuntimeCodeObject) dataRecord.getAttr(TokenType.CONSTR.name().toLowerCase());
      final RuntimeInstance selfObject = allocator.allocateEmptyObject();
      final RuntimeCallable actualCallable = allocator.allocateCallable(hostModule, selfObject, constructor);

      passOver = selfObject;

      return generalCall(allocator, instr, actualCallable, args, "");
    }   
    else {
      return prepareErrorJump(instr, allocator, "Target isn't callable "+callable.getClass());
    }                         
  }

  private StackFrame jump(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    instrIndex = jumpInstr.getArgument() - 1;
    //frame.setInstrIndex(jumpInstr.getArgument());
    //frame.decrmntInstrIndex();
    return this;
  }

  private StackFrame jumpTrue(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    final RuntimeInstance boolValue = popOperand();
    /*
     * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
     * 
     * At the moment, we're using strict typing by checking if the condition value
     * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
     * 
     * Should we do the same?
     */
    if ((boolValue instanceof RuntimeBool) && ((RuntimeBool) boolValue).getValue()) {
      instrIndex = jumpInstr.getArgument() - 1;
      //setInstrIndex(jumpInstr.getArgument());
      //decrmntInstrIndex();
    }

    return this;
  }

  private StackFrame jumpFalse(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    final RuntimeInstance boolValue = popOperand();
    /*
     * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
     * 
     * At the moment, we're using strict typing by checking if the condition value
     * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
     * 
     * Should we do the same?
     */
    if ((boolValue instanceof RuntimeBool) && !((RuntimeBool) boolValue).getValue()) {
      instrIndex = jumpInstr.getArgument() - 1;
      //frame.setInstrIndex(jumpInstr.getArgument());
      //frame.decrmntInstrIndex();
    }
    return this;
  }

  private StackFrame returnFrame(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    returnValue(popOperand());
    return null;
  }

  private StackFrame throwError(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
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

    return prepareErrorJump(instr, allocator, error.getMessage());
  }

  private StackFrame popError(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    pushOperand(getError().getErrorObject());
    returnError(null);
    return this;
  }

  private StackFrame makeArgV(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    pushOperand(new ArgVector());
    return this;
  }

  private StackFrame arg(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction argInstr = (ArgInstruction) instr;          
    //Pop the actual argument
    final RuntimeInstance argValue = popOperand();
    
    ArgVector argVector = (ArgVector) popOperand();

    //System.out.println(" ===> arg instr!");
    
    if (argInstr.getArgument() >= 0) {
      String argName = ((RuntimeString) hostModule.getConstant(argInstr.getArgument())).getValue();
      argVector.setKeywordArg(argName, argValue);

      //System.out.println(" ====> Setting arg keyword "+argName+" | value = "+argValue);
    }
    else {
      argVector.addPositional(argValue);
    }
    
    pushOperand(argVector);
    return this;
  }

  private StackFrame loadConstant(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction loadcInstr = (ArgInstruction) instr;
    RuntimeInstance constant = hostModule.getConstant(loadcInstr.getArgument());
    pushOperand(constant);
    LOG.info(" ==> LOADC "+loadcInstr.getArgument()+" || "+constant);
    return this;
  }

  private StackFrame loadLocal(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;

    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    pushOperand(getLocalVar(loadInstr.getArgument()));
    return this;
  }

  private StackFrame storeLocal(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final RuntimeInstance value = popOperand();


    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    storeLocalVar(storeInstr.getArgument(), value);
    return this;
  }

  private StackFrame loadAttr(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    //final long start = System.nanoTime();
    final ArgInstruction loadInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) hostModule.getConstant(loadInstr.getArgument())).getValue();
    final RuntimeInstance object = popOperand();
    
    //System.out.println("====> object attr: "+object.attrs());

    final RuntimeInstance attrValue = object.getAttr(attrName);
    if(attrValue != null) {
      pushOperand(attrValue);
      //final long end = System.nanoTime();
      //GeneralMetrics.addTimes(Meaures.ATTR_LOOKUP_DIPATCH, end - start);
      return this;
    }
    else {
      //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet()+" | "+object.getClass()+" | "+instr.getStart());
      return prepareErrorJump(instr, allocator, "'"+attrName+"' is unfound on object.");
      //System.out.println("---------> ATTR ERROR DONE!!! ");
    }
  }

  private StackFrame storeAttr(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) hostModule.getConstant(storeInstr.getArgument())).getValue();
    final RuntimeInstance object = popOperand();
    final RuntimeInstance value = popOperand();

    //System.out.println(" ===> STORING ATTR: "+attrName+" | "+object.attrModifiers(attrName));
            
    try {
      object.setAttribute(attrName, value);
      pushOperand(object);
      return this;
    } catch (OperationException e) {
      return prepareErrorJump(instr, allocator, e.getMessage());
    }
  }

  private StackFrame loadNull(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    System.out.println(" => null: "+instr);
    pushOperand(RuntimeNull.NULL);
    return this;
  }

  private StackFrame loadCapture(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;

    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    pushOperand(getCapture(loadInstr.getArgument()));
    return this;
  }

  private StackFrame storeCapture(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final RuntimeInstance value = popOperand();


    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    setCapture(storeInstr.getArgument(), value);
    return this;
  }

  private StackFrame loadModuleVar(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) {
    final long methodStart = System.nanoTime();

    final ArgInstruction loadInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) hostModule.getConstant(loadInstr.getArgument())).getValue();
    final RuntimeInstance moduleObject = hostModule.getModuleObject();
    final RuntimeInstance attrValue = moduleObject.getAttr(attrName);
    
    if(attrValue != null) {
      pushOperand(attrValue);
      GeneralMetrics.addTimes(Meaures.MOD_VAR_LOAD, System.nanoTime() - methodStart);
      return this;
    }
    else {
      //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet());
      return prepareErrorJump(instr, allocator, "'"+attrName+"' is unfound on module.");
    }
  }

  private StackFrame storeModuleVar(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) { 
    final ArgInstruction storeInstr = (ArgInstruction) instr;      
    final RuntimeInstance newValue = popOperand();
    final String attrName = ((RuntimeString) hostModule.getConstant(storeInstr.getArgument())).getValue();
    //System.out.println(">>>> STOREMV: "+attrName+" | "+storeInstr.getIndex());
    final RuntimeInstance moduleObject = hostModule.getModuleObject();
    
    try {
      moduleObject.setAttribute(attrName, newValue);
      return this;
    } catch (OperationException e) {
      return prepareErrorJump(instr, allocator, e.getMessage());
    }
  }

  private StackFrame loadIndex(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) { 
    final RuntimeInstance index = popOperand();
    final RuntimeInstance target = popOperand();

    try {
      final RuntimeInstance result = target.$getAtIndex(index, allocator);
      pushOperand(result);
      return this;
    } catch (OperationException e) {
      return checkAndCall(allocator, 
                          instr, 
                          target, 
                          RuntimeArray.RETR_INDEX_ATTR, 
                          new ArgVector(index), 
                          target.getClass()+" isn't indexible", 
                          target.getClass()+" isn't indexible"); 
    }                                 
  }

  private StackFrame storeIndex(RuntimeInstruction instr, Fiber fiber, HeapAllocator allocator) { 
    final RuntimeInstance value = popOperand();
    final RuntimeInstance index = popOperand();
    final RuntimeInstance target = popOperand();

    try {
      target.$setAtIndex(index, value, allocator);
      pushOperand(RuntimeNull.NULL);
      return this;
    } catch (OperationException e) {
      return checkAndCall(allocator, 
                          instr, 
                          target, 
                          RuntimeArray.STORE_INDEX_ATTR, 
                          new ArgVector(index), 
                          target.getClass()+" isn't indexible", 
                          target.getClass()+" isn't indexible"); 
    }                                
  }

  private StackFrame loadModule(RuntimeInstruction instr,
                                Fiber fiber,
                                HeapAllocator allocator) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;
    if(loadInstr.getArgument() < 0) {
      //Load the instr module
      pushOperand(hostModule.getModuleObject());
      return this;
    }              
    else {
      final String moduleName = ((RuntimeString) hostModule.getConstant(loadInstr.getArgument())).getValue();

      try {
        final RuntimeModule otherModule = fiber.getFinder().load(moduleName);

        //System.out.println(" is "+otherModule.getName()+" loaded? "+otherModule.isLoaded());

        if (!otherModule.isLoaded()) {
          final StackFrame otherModuleFrame = generalCall(allocator, 
                                                          loadInstr, 
                                                          otherModule.getModuleCallable(), 
                                                          new ArgVector(), 
                                                          "");
          pushOperand(otherModule.getModuleObject());
          otherModule.setAsLoaded(true);
          return otherModuleFrame;
        }
        else {
          pushOperand(otherModule.getModuleObject());
          return this;
        }
      } catch (ModuleLoadException e) {
        return prepareErrorJump(instr, allocator, e.getMessage());
      }
    }
  }                                 

  private StackFrame exportModuleVar(RuntimeInstruction instr, 
                                     Fiber fiber,
                                     HeapAllocator allocator) { 
    final ArgInstruction exportInstr = (ArgInstruction) instr;
    final String varName = ((RuntimeString) hostModule.getConstant(exportInstr.getArgument())).getValue();
    final RuntimeInstance moduleObject = hostModule.getModuleObject();

    //System.out.println("===> making module variable "+varName+" export!");

    try {
      moduleObject.appendAttrModifier(varName, AttrModifier.EXPORT);
    } catch (OperationException e) {
      //Should never happen as the IRCompiler should always put a 
      //CONSTMV right after the initial value of a module variable has been set
      throw new Error(e);
    }      
    
    return this;
  }

  private StackFrame constantModuleVar(RuntimeInstruction instr, 
                                       Fiber fiber,
                                       HeapAllocator allocator) { 
    final ArgInstruction exportInstr = (ArgInstruction) instr;
    final String varName = ((RuntimeString) hostModule.getConstant(exportInstr.getArgument())).getValue();

    System.out.println("===> making module variable "+varName+" constant!");
    //System.out.println("===> FOR MODULE: "+System.lineSeparator()+getHostModule());

    final RuntimeInstance value = popOperand();

    final RuntimeInstance moduleObject = hostModule.getModuleObject();
    try {
      moduleObject.setAttribute(varName, value, AttrModifier.CONSTANT);
    } catch (OperationException e) {
      //Should never happen as the IRCompiler should always put a 
      //CONSTMV right after the initial value of a module variable has been set
      throw new Error(e);
    }     
    
    return this;
  }

  private StackFrame allocateFunc(RuntimeInstruction instr, 
                                  Fiber fiber,
                                  HeapAllocator allocator) {
    final RuntimeInstance codeObject = popOperand();
    if (codeObject instanceof RuntimeCodeObject) {
      final RuntimeCodeObject actualCodeObject = (RuntimeCodeObject) codeObject;
      
      //Capture local variables based on the instr frame
      CellReference [] capturedLocals = new CellReference[actualCodeObject.getCaptures().length];
      for(int dest = 0; dest < capturedLocals.length; dest++) {
        capturedLocals[dest] = getCaptureReference(actualCodeObject.getCaptures()[dest]);
      }
      
      //System.out.println(" ---> ALLOCF, CAPTURED LOCAL: "+capturedLocals.length+" "+Arrays.toString(actualCodeObject.getCaptures()));
      
      final RuntimeInstance self = popOperand();
      final RuntimeCallable callable = allocator.allocateCallable(hostModule, self, actualCodeObject, capturedLocals);
      pushOperand(callable);
      return this;
    }
    else {
      return prepareErrorJump(instr, allocator, "Not a code object "+codeObject.getClass());
    }                                    
  }

  private StackFrame allocateArray(RuntimeInstruction instr, 
                                   Fiber fiber,
                                   HeapAllocator allocator) {
    final ArgVector args = (ArgVector) popOperand();
    final RuntimeArray array = allocator.allocateEmptyArray();
    
    for(int i = 0; i < args.getPositionals().size(); i++) {
      //System.out.println("=== ADDING: "+args.getPositional(i));
      array.addValue(args.getPositional(i));
    }
    
    pushOperand(array);       
    
    return this;
  }

  private StackFrame allocateObject(RuntimeInstruction instr, 
                                    Fiber fiber,
                                    HeapAllocator allocator) {
    final ArgInstruction alloco = (ArgInstruction) instr;
    final ArgVector args = (ArgVector) popOperand();
    
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
    
    return this;
  }

  private StackFrame bind(RuntimeInstruction instr, 
                          Fiber fiber,
                          HeapAllocator allocator) {
    Callable func = (Callable) popOperand();
    final RuntimeInstance newSelf = popOperand();

    func = func.rebind(newSelf, allocator);
    pushOperand(func);    
    
    return this;
  }

  private StackFrame makeConstant(RuntimeInstruction instr, 
                                  Fiber fiber,
                                  HeapAllocator allocator) {
    final ArgInstruction hasInstr = (ArgInstruction) instr;

    final RuntimeInstance attrValue = popOperand();
    final RuntimeInstance targetObj = popOperand();
    final String attrName = ((RuntimeString) hostModule.getConstant(hasInstr.getArgument())).getValue();

    try {
      targetObj.setAttribute(attrName, attrValue);
      targetObj.appendAttrModifier(attrName, AttrModifier.CONSTANT);
      pushOperand(targetObj);
    } catch (OperationException e) {
      return prepareErrorJump(hasInstr, allocator, "Cannot make '"+attrName+"' constant: "+e.getMessage());
    }   
    
    return this;
  }

  private StackFrame sealObject(RuntimeInstruction instr, 
                                Fiber fiber,
                                HeapAllocator allocator) {
    final RuntimeInstance obj = popOperand();
    obj.seal();
    pushOperand(obj);  
    
    return this;
  }

  private StackFrame hasKeywordArg(RuntimeInstruction instr, 
                                   Fiber fiber,
                                   HeapAllocator allocator) {
    final ArgInstruction hasInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) hostModule.getConstant(hasInstr.getArgument())).getValue();
    final RuntimeBool result = allocator.allocateBool(initialArgs.hasAttr(attrName));
    //System.out.println(" ===> has k_arg? "+attrName+" | "+initialArgs.attrs()+" | "+result);
    pushOperand(result);  
    
    return this;
  }

  private StackFrame equality(RuntimeInstruction instr, 
                              Fiber fiber,
                              HeapAllocator allocator) {
    final RuntimeInstance right = popOperand();
    final RuntimeInstance left = popOperand();

    if (left == right) {
      pushOperand(allocator.allocateBool(true));
      return this;
    }

    final RuntimeInstance result = RuntimeUtils.numEqual(left, right, allocator);
    if (result != null) {
      pushOperand(result);
      return this;
    }

    FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(instr.getOpCode());
    final String opFuncName = coupling.getFuncName();
    
    if (left.hasAttr(opFuncName)) {
      RuntimeInstance func = left.getAttr(opFuncName);
      if (func instanceof Callable) {
        Callable actualCallable = (Callable) func;     
        ArgVector args = new ArgVector(right);
        
        try {
          StackFrame newFrame = StackFrame.makeFrame(actualCallable, args, allocator);
          instrIndex++;
          return newFrame;
        } catch (CallSiteException e) {
          return prepareErrorJump(instr, allocator, e.getMessage());
        }
      }
      else {
        pushOperand(allocator.allocateBool(false));
      }
    }
    else {
      pushOperand(allocator.allocateBool(false));
    }

    return this;
  }

  private StackFrame checkAndCall(HeapAllocator allocator,
                                  RuntimeInstruction instr,
                                  RuntimeInstance target, 
                                  String funcName, 
                                  ArgVector args,
                                  String notCallableError,
                                  String noAttrError) {
    final RuntimeInstance potentialCallable = target.getAttr(funcName);
    if (potentialCallable == null) {
      //No such attribute
      return prepareErrorJump(instr, allocator, noAttrError);
    }
    else {
      return generalCall(allocator, instr, potentialCallable, args, notCallableError);
    }
  }

  private StackFrame generalCall(HeapAllocator allocator,
                                 RuntimeInstruction instr,
                                 RuntimeInstance potentialCallable, 
                                 ArgVector args,
                                 String notCallableError) {
    if (potentialCallable instanceof Callable) {
      final Callable actualCallable = (Callable) potentialCallable;

      try {
        final RuntimeInstance result = RuntimeUtils.fastCall(null, args, null);
        if (result != null) {
          pushOperand(result);
          return this;
        }
        else {
          final StackFrame newFrame = StackFrame.makeFrame(actualCallable, args, allocator);
          instrIndex++;
          return newFrame;
        }
      } catch (InvocationException | CallSiteException e) {
        return prepareErrorJump(instr, allocator, e.getMessage());
      }
    }
    else return prepareErrorJump(instr, allocator, notCallableError);
  }

  private StackFrame prepareErrorJump(RuntimeInstruction instruction, 
                                      HeapAllocator allocator, 
                                      String errorMessage) {
    final RuntimeError error = allocator.allocateError(errorMessage);
    returnError(error);
    if (instruction.getExceptionJumpIndex() >= 0) {
      instrIndex = instruction.getExceptionJumpIndex();
      return this;
    }
    else {
      return null;
    }
  }

  // Bytecode dispatch methods - END

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
  
  public RuntimeInstruction getCurrInstr() {
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

  public CellReference[] getCaptureReferences() {
    return callable.getCaptures();
  }

  public void setPassOver(RuntimeInstance passOver) {
    this.passOver = passOver;
  }

  public boolean hasPassOver() {
    return passOver != null;
  }
  
  public RuntimeInstance getPassOver() {
    return passOver;
  }

  @Override
  public RuntimeCallable getCallable() {
    return callable;
  }

}
