package jg.sh.runtime.threading.frames;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jg.sh.compile.instrs.OpCode;
import jg.sh.parsing.token.TokenType;
import jg.sh.runtime.alloc.CellReference;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.exceptions.CallSiteException;
import jg.sh.runtime.exceptions.InvocationException;
import jg.sh.runtime.exceptions.OperationException;
import jg.sh.runtime.instrs.ArgInstruction;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.ArgVector;
import jg.sh.runtime.objects.RuntimeArray;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeDataRecord;
import jg.sh.runtime.objects.RuntimeError;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.RuntimeInstance.AttrModifier;
import jg.sh.runtime.objects.RuntimeNull;
import jg.sh.runtime.objects.callable.Callable;
import jg.sh.runtime.objects.callable.RuntimeCallable;
import jg.sh.runtime.objects.literals.FuncOperatorCoupling;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;
import jg.sh.runtime.threading.fiber.Fiber;
import jg.sh.util.RuntimeUtils;

import static jg.sh.compile.instrs.OpCode.*;

import java.util.Arrays;
import java.util.Map.Entry;


public final class BytecodeDispatch {

  private static Logger LOG = LogManager.getLogger(BytecodeDispatch.class);

  @FunctionalInterface
  public static interface Dispatcher {

    public StackFrame dispatcher(RuntimeInstruction instr, Fiber fiber,
                                 FunctionFrame frame, 
                                 HeapAllocator allocator, 
                                 RuntimeModule hostModule);

  }
  
  public static final class Dispatch implements Comparable<Dispatch> {
    final OpCode op;
    final Dispatcher dispatch;

    private Dispatch(OpCode op, Dispatcher dispatch) {
      this.op = op;
      this.dispatch = dispatch;
    }

    @Override
    public int compareTo(Dispatch o) {
      return op.ordinal() - o.op.ordinal();
    }
  }

  public static final Dispatch [] dispatch = {
    new Dispatch(LABEL, null),
    new Dispatch(PASS, null),
    new Dispatch(COMMENT, null),

    new Dispatch(EQUAL, BytecodeDispatch::equality),
    new Dispatch(ADD, BytecodeDispatch::binAdd),
    new Dispatch(SUB, BytecodeDispatch::binMinus),
    new Dispatch(MUL, BytecodeDispatch::binMul),
    new Dispatch(DIV, BytecodeDispatch::binDiv),
    new Dispatch(BAND, BytecodeDispatch::binBitwiseAnd),
    new Dispatch(BOR, BytecodeDispatch::binBitwiseOr),

    new Dispatch(LESS, BytecodeDispatch::binLess),
    new Dispatch(GREAT, BytecodeDispatch::binGreat),
    new Dispatch(LESSE, BytecodeDispatch::binLessEqual),
    new Dispatch(GREATE, BytecodeDispatch::binGreatEqual),
    new Dispatch(MOD, BytecodeDispatch::binMod),

    new Dispatch(INC, BytecodeDispatch::inc),
    new Dispatch(DEC, BytecodeDispatch::dec),

    new Dispatch(NOT, BytecodeDispatch::not),
    new Dispatch(NEG, BytecodeDispatch::negative),

    new Dispatch(CALL, BytecodeDispatch::call),
    new Dispatch(JUMP, BytecodeDispatch::jump),
    new Dispatch(JUMPT, BytecodeDispatch::jumpTrue),
    new Dispatch(JUMPF, BytecodeDispatch::jumpFalse),
    new Dispatch(RET, BytecodeDispatch::returnFrame),
    new Dispatch(RETE, BytecodeDispatch::throwError),
    new Dispatch(POPERR, BytecodeDispatch::popError),
    new Dispatch(MAKEARGV, BytecodeDispatch::makeArgV),
    new Dispatch(ARG, BytecodeDispatch::arg),

    new Dispatch(LOADC, BytecodeDispatch::loadConstant),

    new Dispatch(LOAD, BytecodeDispatch::loadLocal),
    new Dispatch(STORE, BytecodeDispatch::storeLocal),

    new Dispatch(LOADATTR, BytecodeDispatch::loadAttr),
    new Dispatch(STOREATTR, BytecodeDispatch::storeAttr),

    new Dispatch(LOADNULL, BytecodeDispatch::loadNull),

    new Dispatch(LOAD_CL, BytecodeDispatch::loadCapture),
    new Dispatch(STORE_CL, BytecodeDispatch::storeCaputure),

    new Dispatch(LOADMV, BytecodeDispatch::loadModuleVar),
    new Dispatch(STOREMV, BytecodeDispatch::storeModuleVar),
    new Dispatch(LOADIN, BytecodeDispatch::loadIndex),
    new Dispatch(STOREIN, BytecodeDispatch::storeIndex),
    new Dispatch(LOADMOD, BytecodeDispatch::loadModule),

    new Dispatch(EXPORTMV, BytecodeDispatch::exportModuleVar),
    new Dispatch(CONSTMV, BytecodeDispatch::constantModuleVar),

    new Dispatch(ALLOCF, BytecodeDispatch::allocateFunc),
    new Dispatch(ALLOCA, BytecodeDispatch::allocateArray),
    new Dispatch(ALLOCO, BytecodeDispatch::allocateObject),
    new Dispatch(BIND, BytecodeDispatch::bind),
    new Dispatch(MAKECONST, BytecodeDispatch::makeConstant),
    new Dispatch(SEAL, BytecodeDispatch::sealObject),
    new Dispatch(HAS_KARG, BytecodeDispatch::hasKeywordArg),

    new Dispatch(CALLA, null),
    new Dispatch(CAPTURE, null),
    new Dispatch(LADD, null),
    new Dispatch(LOADSELF, null)
  };

  static {
    Arrays.sort(dispatch);
  }

  public static StackFrame binAdd(RuntimeInstruction instr, Fiber fiber,
                                  FunctionFrame frame, 
                                  HeapAllocator allocator, 
                                  RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    /*
     * Check if we're ADDing two objects - one operand being a String.
     * If so, add both instance's toString() result
     */
    if (left instanceof RuntimeString || right instanceof RuntimeString) {
      frame.pushOperand(allocator.allocateString(left.toString() + right.toString()));
      return frame;
    }
    
    final RuntimeInstance result = RuntimeUtils.numAdd(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(ADD), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binMinus(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numMinus(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(SUB), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binMul(RuntimeInstruction instr, Fiber fiber,
                                  FunctionFrame frame, 
                                  HeapAllocator allocator, 
                                  RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numMult(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(MUL), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binDiv(RuntimeInstruction instr, Fiber fiber,
                                  FunctionFrame frame, 
                                  HeapAllocator allocator, 
                                  RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numDiv(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(DIV), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binBitwiseAnd(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      RuntimeInteger lefInt = (RuntimeInteger) left;
      RuntimeInteger rightInt = (RuntimeInteger) right;
      frame.pushOperand(allocator.allocateInt(lefInt.getValue() & rightInt.getValue()));
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(BAND), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binBitwiseOr(RuntimeInstruction instr, Fiber fiber,
                                        FunctionFrame frame, 
                                        HeapAllocator allocator, 
                                        RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    if (left instanceof RuntimeInteger && right instanceof RuntimeInteger) {
      RuntimeInteger lefInt = (RuntimeInteger) left;
      RuntimeInteger rightInt = (RuntimeInteger) right;
      frame.pushOperand(allocator.allocateInt(lefInt.getValue() | rightInt.getValue()));
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(BOR), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binLess(RuntimeInstruction instr, Fiber fiber,
                                   FunctionFrame frame, 
                                   HeapAllocator allocator, 
                                   RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numLess(left, right, false, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(LESS), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binGreat(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numGreat(left, right, false, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(GREAT), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binLessEqual(RuntimeInstruction instr, Fiber fiber,
                                        FunctionFrame frame, 
                                        HeapAllocator allocator, 
                                        RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numLess(left, right, true, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(LESSE), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binGreatEqual(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numGreat(left, right, true, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(GREATE), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame binMod(RuntimeInstruction instr, Fiber fiber,
                                  FunctionFrame frame, 
                                  HeapAllocator allocator, 
                                  RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.numMod(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    return binaryArithCall(frame, 
                           allocator, 
                           FuncOperatorCoupling.getCoupling(MOD), 
                           instr, 
                           left, 
                           right);
  }

  public static StackFrame inc(RuntimeInstruction instr, Fiber fiber,
                               FunctionFrame frame, 
                               HeapAllocator allocator, 
                               RuntimeModule module) {
    final RuntimeInstance target = frame.popOperand();
    
    if(target instanceof RuntimeInteger){
      RuntimeInteger integer = (RuntimeInteger) target;
      integer = allocator.allocateInt(integer.getValue() + 1);
      frame.pushOperand(integer); 
      return frame;
    }
    else if(target instanceof RuntimeFloat) {
      RuntimeFloat floatingPoint = (RuntimeFloat) target;
      floatingPoint = allocator.allocateFloat(floatingPoint.getValue() + 1.0);
      frame.pushOperand(floatingPoint); 
      return frame;
    }
    else {
      final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(OpCode.ADD);
      return binaryArithCall(frame, 
                             allocator, 
                             coupling, 
                             instr, 
                             target, 
                             allocator.allocateInt(1));
    }
  }

  public static StackFrame negative(RuntimeInstruction instr, Fiber fiber,
                               FunctionFrame frame, 
                               HeapAllocator allocator, 
                               RuntimeModule module) {
    final RuntimeInstance target = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.negative(target, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(instr.getOpCode());
    return checkAndCall(frame, 
                        allocator, 
                        instr, 
                        target, 
                        coupling.getFuncName(), 
                        new ArgVector(), 
                        "Value for "+coupling.getOpCode().name().toLowerCase()+" isn't callable.", 
                        "Unsupported operator for "+coupling.getOpCode().name().toLowerCase()+" isn't supported for "+target.getClass());
  }

  public static StackFrame not(RuntimeInstruction instr, Fiber fiber,
                               FunctionFrame frame, 
                               HeapAllocator allocator, 
                               RuntimeModule module) {
    final RuntimeInstance target = frame.popOperand();
    
    final RuntimeInstance result = RuntimeUtils.negate(target, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
    }

    final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(instr.getOpCode());
    return checkAndCall(frame, 
                        allocator, 
                        instr, 
                        target, 
                        coupling.getFuncName(), 
                        new ArgVector(), 
                        "Value for "+coupling.getOpCode().name().toLowerCase()+" isn't callable.", 
                        "Unsupported operator for "+coupling.getOpCode().name().toLowerCase()+" isn't supported for "+target.getClass());
  }

  public static StackFrame dec(RuntimeInstruction instr, Fiber fiber,
                                  FunctionFrame frame, 
                                  HeapAllocator allocator, 
                                  RuntimeModule module) {
    final RuntimeInstance target = frame.popOperand();
    
    if(target instanceof RuntimeInteger){
      RuntimeInteger integer = (RuntimeInteger) target;
      integer = allocator.allocateInt(integer.getValue() - 1);
      frame.pushOperand(integer); 
      return frame;
    }
    else if(target instanceof RuntimeFloat) {
      RuntimeFloat floatingPoint = (RuntimeFloat) target;
      floatingPoint = allocator.allocateFloat(floatingPoint.getValue() - 1.0);
      frame.pushOperand(floatingPoint); 
      return frame;
    }
    else {
      final FuncOperatorCoupling coupling = FuncOperatorCoupling.getCoupling(OpCode.SUB);
      return binaryArithCall(frame, 
                             allocator, 
                             coupling, 
                             instr, 
                             target, 
                             allocator.allocateInt(1));
    }
  }

  public static StackFrame binaryArithCall(FunctionFrame caller, 
                                           HeapAllocator allocator, 
                                           FuncOperatorCoupling opFuncName,
                                           RuntimeInstruction instruction, 
                                           RuntimeInstance left, 
                                           RuntimeInstance right) {
    LOG.trace(" For: "+instruction+" => Not numerical operands. Will call overloaded operator implementation!");
    return checkAndCall(caller, 
                        allocator, 
                        instruction, 
                        right, 
                        opFuncName.getFuncName(), 
                        new ArgVector(right), 
                        opFuncName.getOpCode().name().toLowerCase()+" isn't a callable", 
                        "Unsupported operation for "+opFuncName.getOpCode().name().toLowerCase()+" on "+left.getClass());
  }

  public static StackFrame call(RuntimeInstruction instr, Fiber fiber,
                                FunctionFrame frame, 
                                HeapAllocator allocator, 
                                RuntimeModule module) {
    final RuntimeInstance callable = frame.popOperand();
    final ArgVector args = (ArgVector) frame.popOperand();   
    
    if (callable instanceof Callable) {
      return generalCall(frame, allocator, instr, callable, args, "");
    }
    else if(callable instanceof RuntimeDataRecord) {
      final RuntimeDataRecord dataRecord = (RuntimeDataRecord) callable;
      LOG.info(" ===> call to data record!!!");

      final RuntimeCodeObject constructor = (RuntimeCodeObject) dataRecord.getAttr(TokenType.CONSTR.name().toLowerCase());
      final RuntimeInstance selfObject = allocator.allocateEmptyObject();
      final RuntimeCallable actualCallable = allocator.allocateCallable(frame.getHostModule(), selfObject, constructor);

      return generalCall(frame, allocator, instr, actualCallable, args, "");
    }   
    else {
      return prepareErrorJump(frame, instr, allocator, "Target isn't callable "+callable.getClass());
    }                         
  }

  public static StackFrame jump(RuntimeInstruction instr, Fiber fiber,
                                FunctionFrame frame, 
                                HeapAllocator allocator, 
                                RuntimeModule module) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    frame.setInstrIndex(jumpInstr.getArgument());
    frame.decrmntInstrIndex();
    return frame;
  }

  public static StackFrame jumpTrue(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    final RuntimeInstance boolValue = frame.popOperand();
    /*
     * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
     * 
     * At the moment, we're using strict typing by checking if the condition value
     * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
     * 
     * Should we do the same?
     */
    if ((boolValue instanceof RuntimeBool) && ((RuntimeBool) boolValue).getValue()) {
      frame.setInstrIndex(jumpInstr.getArgument());
      frame.decrmntInstrIndex();
    }
    return frame;
  }

  public static StackFrame jumpFalse(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) {
    final ArgInstruction jumpInstr = (ArgInstruction) instr;
    final RuntimeInstance boolValue = frame.popOperand();
    /*
     * TODO: Many dynamic languages implement the idea of "truthy" and "falsy" values.
     * 
     * At the moment, we're using strict typing by checking if the condition value
     * is a boolean. However, other dynamic languages are much more flexible by using falsy/truthy values
     * 
     * Should we do the same?
     */
    if ((boolValue instanceof RuntimeBool) && !((RuntimeBool) boolValue).getValue()) {
      frame.setInstrIndex(jumpInstr.getArgument());
      frame.decrmntInstrIndex();
    }
    return frame;
  }

  public static StackFrame returnFrame(RuntimeInstruction instr, Fiber fiber,
                                       FunctionFrame frame, 
                                       HeapAllocator allocator, 
                                       RuntimeModule module) {
    frame.returnValue(frame.popOperand());
    return frame;
  }

  public static StackFrame throwError(RuntimeInstruction instr, Fiber fiber,
                                      FunctionFrame frame, 
                                      HeapAllocator allocator, 
                                      RuntimeModule module) {
    RuntimeInstance potentialException = frame.popOperand();
          
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

    return prepareErrorJump(frame, instr, allocator, error.getMessage());
  }

  public static StackFrame popError(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    frame.pushOperand(frame.getError().getErrorObject());
    frame.returnError(null);
    return frame;
  }

  public static StackFrame makeArgV(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    frame.pushOperand(new ArgVector());
    return frame;
  }

  public static StackFrame arg(RuntimeInstruction instr, Fiber fiber,
                               FunctionFrame frame, 
                               HeapAllocator allocator, 
                               RuntimeModule module) {
    final ArgInstruction argInstr = (ArgInstruction) instr;          
    //Pop the actual argument
    final RuntimeInstance argValue = frame.popOperand();
    
    ArgVector argVector = (ArgVector) frame.popOperand();

    //System.out.println(" ===> arg instr!");
    
    if (argInstr.getArgument() >= 0) {
      String argName = ((RuntimeString) frame.getHostModule().getConstantMap().get(argInstr.getArgument())).getValue();
      argVector.setKeywordArg(argName, argValue);

      //System.out.println(" ====> Setting arg keyword "+argName+" | value = "+argValue);
    }
    else {
      argVector.addPositional(argValue);
    }
    
    frame.pushOperand(argVector);
    return frame;
  }

  public static StackFrame loadConstant(RuntimeInstruction instr, Fiber fiber,
                                        FunctionFrame frame, 
                                        HeapAllocator allocator, 
                                        RuntimeModule module) {
    final ArgInstruction loadcInstr = (ArgInstruction) instr;
    RuntimeInstance constant = frame.getHostModule().getConstantMap().get(loadcInstr.getArgument());
    frame.pushOperand(constant);
    LOG.info(" ==> LOADC "+loadcInstr.getArgument()+" || "+constant);
    return frame;
  }

  public static StackFrame loadLocal(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;

    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    frame.pushOperand(frame.getLocalVar(loadInstr.getArgument()));
    return frame;
  }

  public static StackFrame storeLocal(RuntimeInstruction instr, Fiber fiber,
                                      FunctionFrame frame, 
                                      HeapAllocator allocator, 
                                      RuntimeModule module) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final RuntimeInstance value = frame.popOperand();


    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    frame.storeLocalVar(storeInstr.getArgument(), value);
    return frame;
  }

  public static StackFrame loadAttr(RuntimeInstruction instr, Fiber fiber,
                                    FunctionFrame frame, 
                                    HeapAllocator allocator, 
                                    RuntimeModule module) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) frame.getHostModule().getConstantMap().get(loadInstr.getArgument())).getValue();
    final RuntimeInstance object = frame.popOperand();
    
    //System.out.println("====> object attr: "+object.attrs());

    if(object.hasAttr(attrName)) {
      frame.pushOperand(object.getAttr(attrName));
      return frame;
    }
    else {
      //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet()+" | "+object.getClass()+" | "+instr.getStart());
      return prepareErrorJump(frame, instr, allocator, "'"+attrName+"' is unfound on object.");
      //System.out.println("---------> ATTR ERROR DONE!!! ");
    }
  }

  public static StackFrame storeAttr(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) frame.getHostModule().getConstantMap().get(storeInstr.getArgument())).getValue();
    final RuntimeInstance object = frame.popOperand();
    final RuntimeInstance value = frame.popOperand();

    //System.out.println(" ===> STORING ATTR: "+attrName+" | "+object.attrModifiers(attrName));
            
    try {
      object.setAttribute(attrName, value);
      frame.pushOperand(object);
      return frame;
    } catch (OperationException e) {
      return prepareErrorJump(frame, instr, allocator, e.getMessage());
    }
  }

  public static StackFrame loadNull(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) {
    System.out.println(" => null: "+instr);
    frame.pushOperand(RuntimeNull.NULL);
    return frame;
  }

  public static StackFrame loadCapture(RuntimeInstruction instr, Fiber fiber,
                                       FunctionFrame frame, 
                                       HeapAllocator allocator, 
                                       RuntimeModule module) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;

    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    frame.pushOperand(frame.getCapture(loadInstr.getArgument()));
    return frame;
  }

  public static StackFrame storeCaputure(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final ArgInstruction storeInstr = (ArgInstruction) instr;
    final RuntimeInstance value = frame.popOperand();


    //System.out.println(" ------- LOAD: LOCAL VARS: "+getLocalVars().length+", "+loadInstr.getIndex()+" | AT: "+hashCode());
    frame.setCapture(storeInstr.getArgument(), value);
    return frame;
  }

  public static StackFrame loadModuleVar(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) frame.getHostModule().getConstantMap().get(loadInstr.getArgument())).getValue();
    
    final RuntimeInstance moduleObject = frame.getHostModule().getModuleObject();
    
    if(moduleObject.hasAttr(attrName)) {
      frame.pushOperand(moduleObject.getAttr(attrName));
      return frame;
    }
    else {
      //System.out.println("---------> ATTR ERROR!!! "+"'"+attrName+"' is unfound on object. "+object.getAttributes().keySet());
      return prepareErrorJump(frame, instr, allocator, "'"+attrName+"' is unfound on module.");
    }
  }

  public static StackFrame storeModuleVar(RuntimeInstruction instr, Fiber fiber,
                                          FunctionFrame frame, 
                                          HeapAllocator allocator, 
                                          RuntimeModule module) { 
    final ArgInstruction storeInstr = (ArgInstruction) instr;      
    final RuntimeInstance newValue = frame.popOperand();
    final String attrName = ((RuntimeString) frame.getHostModule().getConstantMap().get(storeInstr.getArgument())).getValue();
    //System.out.println(">>>> STOREMV: "+attrName+" | "+storeInstr.getIndex());
    final RuntimeInstance moduleObject = frame.getHostModule().getModuleObject();
    
    try {
      moduleObject.setAttribute(attrName, newValue);
      return frame;
    } catch (OperationException e) {
      return prepareErrorJump(frame, instr, allocator, e.getMessage());
    }
  }

  public static StackFrame loadIndex(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) { 
    final RuntimeInstance index = frame.popOperand();
    final RuntimeInstance target = frame.popOperand();

    return checkAndCall(frame, 
                        allocator, 
                        instr, 
                        target, 
                        RuntimeArray.RETR_INDEX_ATTR, 
                        new ArgVector(index), 
                        target.getClass()+" isn't indexible", 
                        target.getClass()+" isn't indexible");                                 
  }

  public static StackFrame storeIndex(RuntimeInstruction instr, Fiber fiber,
                                      FunctionFrame frame, 
                                      HeapAllocator allocator, 
                                      RuntimeModule module) { 
    final RuntimeInstance value = frame.popOperand();
    final RuntimeInstance index = frame.popOperand();
    final RuntimeInstance target = frame.popOperand();

    return checkAndCall(frame, 
                        allocator, 
                        instr, 
                        target, 
                        RuntimeArray.RETR_INDEX_ATTR, 
                        new ArgVector(index, value), 
                        target.getClass()+" isn't indexible", 
                        target.getClass()+" isn't indexible");                                 
  }

  public static StackFrame loadModule(RuntimeInstruction instr,
                                      Fiber fiber,
                                      FunctionFrame frame, 
                                      HeapAllocator allocator, 
                                      RuntimeModule module) {
    final ArgInstruction loadInstr = (ArgInstruction) instr;
    if(loadInstr.getArgument() < 0) {
      //Load the instr module
      frame.pushOperand(frame.getHostModule().getModuleObject());
      return frame;
    }              
    else {
      final String moduleName = ((RuntimeString) module.getConstantMap().get(loadInstr.getArgument())).getValue();
      final RuntimeModule otherModule = fiber.getFinder().load(moduleName);

      if (otherModule != null) {
        if (!module.isLoaded()) {
          final StackFrame otherModuleFrame = generalCall(frame, 
                                                          allocator, 
                                                          loadInstr, 
                                                          otherModule.getModuleCallable(), 
                                                          new ArgVector(), 
                                                          "");
          otherModule.setAsLoaded(true);
          frame.pushOperand(otherModule.getModuleObject());
          return otherModuleFrame;
        }
        else {
          frame.pushOperand(otherModule.getModuleObject());
          return frame;
        }
      }
      else {
        return prepareErrorJump(frame, instr, allocator, "Couldn't find the module '"+moduleName+"'");
      }
    }
  }                                 

  public static StackFrame exportModuleVar(RuntimeInstruction instr, Fiber fiber,
                                           FunctionFrame frame, 
                                           HeapAllocator allocator, 
                                           RuntimeModule module) { 
    final ArgInstruction exportInstr = (ArgInstruction) instr;
    final String varName = ((RuntimeString) module.getConstantMap().get(exportInstr.getArgument())).getValue();
    final RuntimeInstance moduleObject = module.getModuleObject();

    //System.out.println("===> making module variable "+varName+" export!");

    try {
      moduleObject.appendAttrModifier(varName, AttrModifier.EXPORT);
    } catch (OperationException e) {
      //Should never happen as the IRCompiler should always put a 
      //CONSTMV right after the initial value of a module variable has been set
      throw new Error(e);
    }      
    
    return frame;
  }

  public static StackFrame constantModuleVar(RuntimeInstruction instr, Fiber fiber,
                                             FunctionFrame frame, 
                                             HeapAllocator allocator, 
                                             RuntimeModule module) { 
    final ArgInstruction exportInstr = (ArgInstruction) instr;
    final String varName = ((RuntimeString) module.getConstantMap().get(exportInstr.getArgument())).getValue();

    //System.out.println("===> making module variable "+varName+" constant!");

    final RuntimeInstance moduleObject = module.getModuleObject();
    try {
      moduleObject.appendAttrModifier(varName, AttrModifier.CONSTANT);
    } catch (OperationException e) {
      //Should never happen as the IRCompiler should always put a 
      //CONSTMV right after the initial value of a module variable has been set
      throw new Error(e);
    }     
    
    return frame;
  }

  public static StackFrame allocateFunc(RuntimeInstruction instr, Fiber fiber,
                                        FunctionFrame frame, 
                                        HeapAllocator allocator, 
                                        RuntimeModule module) {
    final RuntimeInstance codeObject = frame.popOperand();
    if (codeObject instanceof RuntimeCodeObject) {
      RuntimeCodeObject actualCodeObject = (RuntimeCodeObject) codeObject;
      
      //Capture local variables based on the instr frame
      CellReference [] capturedLocals = new CellReference[actualCodeObject.getCaptures().length];
      for(int dest = 0; dest < capturedLocals.length; dest++) {
        capturedLocals[dest] = frame.getCaptureReference(actualCodeObject.getCaptures()[dest]);
      }
      
      //System.out.println(" ---> ALLOCF, CAPTURED LOCAL: "+capturedLocals.length+" "+Arrays.toString(actualCodeObject.getCaptures()));
      
      RuntimeInstance self = frame.popOperand();
      RuntimeCallable callable = allocator.allocateCallable(module, self, actualCodeObject, capturedLocals);
      frame.pushOperand(callable);
      return frame;
    }
    else {
      return prepareErrorJump(frame, instr, allocator, "Not a code object "+codeObject.getClass());
    }                                    
  }

  public static StackFrame allocateArray(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final ArgVector args = (ArgVector) frame.popOperand();
    final RuntimeArray array = allocator.allocateEmptyArray();
    
    for(int i = 0; i < args.getPositionals().size(); i++) {
      //System.out.println("=== ADDING: "+args.getPositional(i));
      array.addValue(args.getPositional(i));
    }
    
    frame.pushOperand(array);       
    
    return frame;
  }

  public static StackFrame allocateObject(RuntimeInstruction instr, Fiber fiber,
                                          FunctionFrame frame, 
                                          HeapAllocator allocator, 
                                          RuntimeModule module) {
    final ArgInstruction alloco = (ArgInstruction) instr;
    final ArgVector args = (ArgVector) frame.popOperand();
    
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

    frame.pushOperand(object);     
    
    return frame;
  }

  public static StackFrame bind(RuntimeInstruction instr, Fiber fiber,
                                FunctionFrame frame, 
                                HeapAllocator allocator, 
                                RuntimeModule module) {
    Callable func = (Callable) frame.popOperand();
    final RuntimeInstance newSelf = frame.popOperand();

    func = func.rebind(newSelf, allocator);
    frame.pushOperand(func);    
    
    return frame;
  }

  public static StackFrame makeConstant(RuntimeInstruction instr, Fiber fiber,
                                        FunctionFrame frame, 
                                        HeapAllocator allocator, 
                                        RuntimeModule module) {
    final ArgInstruction hasInstr = (ArgInstruction) instr;

    final RuntimeInstance attrValue = frame.popOperand();
    final RuntimeInstance targetObj = frame.popOperand();
    final String attrName = ((RuntimeString) module.getConstantMap().get(hasInstr.getArgument())).getValue();

    try {
      targetObj.setAttribute(attrName, attrValue);
      targetObj.appendAttrModifier(attrName, AttrModifier.CONSTANT);
      frame.pushOperand(targetObj);
    } catch (OperationException e) {
      return prepareErrorJump(frame, hasInstr, allocator, "Cannot make '"+attrName+"' constant: "+e.getMessage());
    }   
    
    return frame;
  }

  public static StackFrame sealObject(RuntimeInstruction instr, Fiber fiber,
                                      FunctionFrame frame, 
                                      HeapAllocator allocator, 
                                      RuntimeModule module) {
    final RuntimeInstance obj = frame.popOperand();
    obj.seal();
    frame.pushOperand(obj);  
    
    return frame;
  }

  public static StackFrame hasKeywordArg(RuntimeInstruction instr, Fiber fiber,
                                         FunctionFrame frame, 
                                         HeapAllocator allocator, 
                                         RuntimeModule module) {
    final ArgInstruction hasInstr = (ArgInstruction) instr;
    final String attrName = ((RuntimeString) module.getConstantMap().get(hasInstr.getArgument())).getValue();
    final RuntimeBool result = allocator.allocateBool(frame.initialArgs.hasAttr(attrName));
    //System.out.println(" ===> has k_arg? "+attrName+" | "+initialArgs.attrs()+" | "+result);
    frame.pushOperand(result);  
    
    return frame;
  }

  private static StackFrame equality(RuntimeInstruction instr, Fiber fiber,
                                     FunctionFrame frame, 
                                     HeapAllocator allocator, 
                                     RuntimeModule module) {
    final RuntimeInstance right = frame.popOperand();
    final RuntimeInstance left = frame.popOperand();

    if (left == right) {
      frame.pushOperand(allocator.allocateBool(true));
      return frame;
    }

    final RuntimeInstance result = RuntimeUtils.numEqual(left, right, allocator);
    if (result != null) {
      frame.pushOperand(result);
      return frame;
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
          frame.incrmntInstrIndex();
          return newFrame;
        } catch (CallSiteException e) {
          return prepareErrorJump(frame, instr, allocator, e.getMessage());
        }
      }
      else {
        frame.pushOperand(allocator.allocateBool(false));
      }
    }
    else {
      frame.pushOperand(allocator.allocateBool(false));
    }

    return frame;
  }

  private static StackFrame checkAndCall(FunctionFrame callerFrame,
                                         HeapAllocator allocator,
                                         RuntimeInstruction instr,
                                         RuntimeInstance target, 
                                         String funcName, 
                                         ArgVector args,
                                         String notCallableError,
                                         String noAttrError) {
    final RuntimeInstance potentialCallable = target.getAttr(funcName);
    if (potentialCallable == null) {
      //No such attribute
      return prepareErrorJump(callerFrame, instr, allocator, noAttrError);
    }
    else {
      return generalCall(callerFrame, allocator, instr, potentialCallable, args, notCallableError);
    }
  }

  private static StackFrame generalCall(FunctionFrame callerFrame,
                                         HeapAllocator allocator,
                                         RuntimeInstruction instr,
                                         RuntimeInstance potentialCallable, 
                                         ArgVector args,
                                         String notCallableError) {
    if (potentialCallable instanceof Callable) {
      final Callable actualCallable = (Callable) potentialCallable;

      try {
        final RuntimeInstance result = RuntimeUtils.fastCall(null, args, null);
        if (result != null) {
          callerFrame.pushOperand(result);
          return callerFrame;
        }
        else {
          final StackFrame newFrame = StackFrame.makeFrame(actualCallable, args, allocator);
          callerFrame.incrmntInstrIndex();
          return newFrame;
        }
      } catch (InvocationException | CallSiteException e) {
        return prepareErrorJump(callerFrame, instr, allocator, e.getMessage());
      }
    }
    else return prepareErrorJump(callerFrame, instr, allocator, notCallableError);
  }

  private static StackFrame prepareErrorJump(FunctionFrame frame, 
                                             RuntimeInstruction instruction, 
                                             HeapAllocator allocator, 
                                             String errorMessage) {
    final RuntimeError error = allocator.allocateError(errorMessage);
    frame.returnError(error);
    if (instruction.getExceptionJumpIndex() >= 0) {
      frame.setInstrIndex(instruction.getExceptionJumpIndex());
      return frame;
    }
    else {
      return null;
    }
  }
}
