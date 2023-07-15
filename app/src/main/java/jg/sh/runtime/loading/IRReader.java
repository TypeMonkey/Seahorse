package jg.sh.runtime.loading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import jg.sh.SeaHorseInterpreter;
import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.CommentInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.JumpInstr;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.LoadCellInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.instrs.StoreCellInstr;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.util.StringUtils;

public class IRReader {
  
  public static final String VERSION = "version";
  public static final String POOL = "pool";
  public static final String TYPE = "type";
  public static final String VALUE = "value";
  public static final String INSTRS = "instrs";
  public static final String SIGNATURE = "signature";
  public static final String BOUND_NAME = "boundName";
  public static final String KEYWORD_INDICES = "keywordIndexes";
  public static final String CAPTURES = "captures";
  public static final String MODIFIERS = "modifiers";
  public static final String POSITIONAL_CNT = "positionalCount";
  public static final String KEYWORD_PARAMS = "keywordParams";
  public static final String HAS_VAR_PARAMS = "hasVarParams";
  public static final String OPCODE = "opcode";
  public static final String ARG = "arg";
  public static final String MODULE_LABEL = "moduleLabel";
  public static final String LINE = "line";
  public static final String COL = "col";
  public static final String ERR_JUMP = "errJmp";
  public static final String INDEX = "index";
  
  public static final String INT = "int";
  public static final String FLOAT = "float";
  public static final String BOOL = "bool";
  public static final String STRING = "string";
  public static final String CODE = "code";

  public static RuntimeModule loadFromSHRCFile(HeapAllocator allocator, File path) throws IOException {
    //System.out.println("---reading shrc file: "+path+" | "+path.isFile()+" | "+path.canRead());
    
    BufferedReader reader = new BufferedReader(new FileReader(path));
    JsonObject object = JsonParser.parseReader(reader).getAsJsonObject();
    reader.close();
    
    //System.out.println(object.entrySet());
    
    //For now, versions must match exactly
    if (object.get(VERSION).getAsInt() != SeaHorseInterpreter.VERSION) {
      return null;
    }
    
    JsonArray pool = object.get(POOL).getAsJsonArray();
    HashMap<Integer, RuntimeInstance> actualPool = new HashMap<>();
    for(int i = 0; i < pool.size(); i++) {
      JsonObject poolComponent = pool.get(i).getAsJsonObject();
      
      JsonObject poolCompValue = poolComponent.get(VALUE).getAsJsonObject();
      int poolIndex = poolComponent.get(INDEX).getAsInt();
      
      actualPool.put(poolIndex, allocatePoolComponent(allocator, poolCompValue));
    }
    
    JsonArray rawInstrs = object.get(INSTRS).getAsJsonArray();
    ContextualInstr [] instrs = new ContextualInstr[rawInstrs.size()];
    for(int i = 0; i < rawInstrs.size(); i++) {
      instrs[i] = parseInstruction(rawInstrs.get(i).getAsJsonObject());
    }
        
    final String moduleName = StringUtils.getBareFileName(path.getName());   
    final RuntimeCodeObject moduleCodeObject = new RuntimeCodeObject(moduleName, 
                               new FunctionSignature(0, Collections.emptySet()), 
                               Collections.emptyMap(), 
                               instrs, 
                               new int[0]);
    
    //System.out.println("----LOADING MODULE .shrc "+path);
    
    return new RuntimeModule(moduleName, 
                             moduleCodeObject, 
                             actualPool);
  }
  
  public static RuntimeCodeObject allocateCodeObject(JsonObject object) {
    //System.out.println("CODE OBJECT: "+object);
    final String boundName = object.get(BOUND_NAME).getAsString();
    
    HashMap<String, Integer> keywordIndexMap = new HashMap<>();
    JsonArray rawKeywordArray = object.get(KEYWORD_INDICES).getAsJsonArray();
    for(int i = 0; i < rawKeywordArray.size(); i++) {
      JsonArray rawEntry = rawKeywordArray.get(i).getAsJsonArray();
      keywordIndexMap.put(rawEntry.get(0).getAsString(), rawEntry.get(0).getAsInt());
    }
    
    JsonArray rawInstrArray = object.get(INSTRS).getAsJsonArray();
    ContextualInstr [] instrs = new ContextualInstr[rawInstrArray.size()];
    for(int i = 0; i < rawInstrArray.size(); i++) {
      instrs[i] = parseInstruction(rawInstrArray.get(i).getAsJsonObject());
    }
    
    JsonArray rawCaptures = object.get(CAPTURES).getAsJsonArray();
    int [] captures = new int[rawCaptures.size()];
    for(int i = 0; i < rawCaptures.size(); i++) {
      captures[i] = rawCaptures.get(i).getAsInt();
    }
    
    FunctionSignature signature = parseSignature(object.get(SIGNATURE).getAsJsonObject());
    return new RuntimeCodeObject(boundName, signature, keywordIndexMap, instrs, captures);
  }
  
  public static FunctionSignature parseSignature(JsonObject object) {
    /*
    HashSet<ReservedWords> modifiers = new HashSet<>();  
    JsonArray rawModifiers = object.get(MODIFIERS).getAsJsonArray();
    for(int i = 0; i < rawModifiers.size(); i++) {
      modifiers.add(ReservedWords.values()[rawModifiers.get(i).getAsInt()]);
    }
    */
    
    final int positionalCount = object.get(POSITIONAL_CNT).getAsInt();
    
    HashSet<String> keywordParams = new HashSet<>();
    JsonArray rawKeywords = object.get(KEYWORD_PARAMS).getAsJsonArray();
    for(int i = 0; i < rawKeywords.size(); i++) {
      keywordParams.add(rawKeywords.get(i).getAsString());
    }
    
    final boolean hasVariableArgs = object.get(HAS_VAR_PARAMS).getAsBoolean();
    return new FunctionSignature(positionalCount, keywordParams, hasVariableArgs, false);
  }
  
  public static RuntimeInstance allocatePoolComponent(HeapAllocator allocator, JsonObject object) {
    //System.out.println("POOL COMP: "+object);
    String type = object.get(TYPE).getAsString();
    
    switch (type) {
    case STRING:
      return allocator.allocateString(object.get(VALUE).getAsString());
    case INT:
      return allocator.allocateInt(object.get(VALUE).getAsLong());
    case FLOAT:
      return allocator.allocateFloat(object.get(VALUE).getAsDouble());
    case BOOL:
      return allocator.allocateBool(object.get(VALUE).getAsBoolean());
    case CODE:
      return allocateCodeObject(object.get(VALUE).getAsJsonObject());
    default:
      return null;
    }
  }
  
  public static ContextualInstr parseInstruction(JsonObject instr) {
    int opCodeOrdinal = instr.get(OPCODE).getAsInt();
    
    OpCode opCode = OpCode.values()[opCodeOrdinal];
    int line = instr.get(LINE).getAsInt();
    int col = instr.get(COL).getAsInt();
    int errJump = instr.get(ERR_JUMP).getAsInt();

    final Location location = new Location(line, col);
    
    Instruction actualInstr = null;
    
    switch (opCode) {
    case LABEL:
      actualInstr = new LabelInstr(location, location, instr.get(ARG).getAsString());
      break;
    case COMMENT:
      actualInstr = new CommentInstr(location, location, instr.get(ARG).getAsString());
      break;
    case PASS:
      actualInstr = new NoArgInstr(location, location, opCode);
      break;
    case EQUAL:
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
    case MOD:
    
    //unary operators
    case NOT:
    case NEG:
      
    //call-related operator
    case CALL:
    case RET:
    case RETE:
    case MAKEARGV:
      
    //Load/store instructions
    case LOADIN:
    case STOREIN:
    case LOADNULL:
      
    case POPERR:
      
    case ALLOCF:
    case ALLOCA:
    case ALLOCO:
      actualInstr = new NoArgInstr(location, location, opCode);
      break;
    
    case JUMP:
    case JUMPT:
    case JUMPF:
      actualInstr = new IndexedJumpInstr(new JumpInstr(location, location, opCode, null), 
                                         instr.get(ARG).getAsInt());
      break;
    
    case EXPORTMV:
    case CONSTMV:
    case ARG:
      actualInstr = new ArgInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
      
    case LOADC:
      actualInstr = new ArgInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case LOAD:
      actualInstr = new LoadCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case STORE:
      actualInstr = new StoreCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case LOADATTR:
      actualInstr = new LoadCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case STOREATTR:
      actualInstr = new StoreCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case LOAD_CL:
      actualInstr = new LoadCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case STORE_CL:
      actualInstr = new StoreCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case LOADMV:
      actualInstr = new LoadCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    case STOREMV:
      actualInstr = new StoreCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;      
    case LOADMOD:
      actualInstr = new LoadCellInstr(location, location, opCode, instr.get(ARG).getAsInt());
      break;
    default:
      break;
    }
    
    return new ContextualInstr(actualInstr, errJump);
  }
}
