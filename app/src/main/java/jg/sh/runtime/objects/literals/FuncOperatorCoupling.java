package jg.sh.runtime.objects.literals;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import jg.sh.compile.instrs.OpCode;

public enum FuncOperatorCoupling {

  ADD("$add", OpCode.ADD),
  SUB("$sub", OpCode.SUB),
  MUL("$mul", OpCode.MUL),
  DIV("$div", OpCode.DIV),
  MOD("$mod", OpCode.MOD),
  NEG("$neg", OpCode.NEG),

  LESS("$less", OpCode.LESS),
  GREAT("$great", OpCode.GREAT),
  LESSE("$lesse", OpCode.LESSE),
  GREATE("$greate", OpCode.GREATE),
  EQUAL("$equal", OpCode.EQUAL),
  NOTEQUAL("$notequal", OpCode.NOTEQUAL),
  
  NOT("$not", OpCode.NOT),
  
  BAND("$band", OpCode.BAND),
  BOR("$bor", OpCode.BOR);
  
  private static final Map<OpCode, FuncOperatorCoupling> opCodeToCoupling;
  
  static {
    HashMap<OpCode, FuncOperatorCoupling> temp = new HashMap<>();
    
    for(FuncOperatorCoupling coupling : FuncOperatorCoupling.values()) {
      temp.put(coupling.getOpCode(), coupling);
    }
    
    opCodeToCoupling = Collections.unmodifiableMap(temp);
  }
  
  private final String funcName;
  private final OpCode opCode;
  
  private FuncOperatorCoupling(String funcName, OpCode opCode){
    this.funcName = funcName;
    this.opCode = opCode;
  }
  
  public String getFuncName() {
    return funcName;
  }
  
  public OpCode getOpCode() {
    return opCode;
  }
  
  public static FuncOperatorCoupling getCoupling(OpCode opCode) {
    return opCodeToCoupling.get(opCode);
  }
}
