package jg.sh.irgen;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import jg.sh.InterpreterOptions;
import jg.sh.SeaHorseInterpreter;
import jg.sh.common.FunctionSignature;
import jg.sh.compile.SeahorseCompiler;
import jg.sh.compile.parsing.nodes.atoms.constructs.Module;
import jg.sh.irgen.instrs.ArgInstr;
import jg.sh.irgen.instrs.CommentInstr;
import jg.sh.irgen.instrs.Instruction;
import jg.sh.irgen.instrs.JumpInstr;
import jg.sh.irgen.instrs.LabelInstr;
import jg.sh.irgen.instrs.LoadCellInstr;
import jg.sh.irgen.instrs.NoArgInstr;
import jg.sh.irgen.instrs.StoreCellInstr;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.loading.ContextualInstr;
import jg.sh.runtime.loading.IndexedJumpInstr;
import jg.sh.runtime.loading.ModuleFinder;
import jg.sh.runtime.loading.RuntimeModule;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;

public class IRWriter {
  
  public static final String VERSION = "\"version\"";
  public static final String POOL = "\"pool\"";
  public static final String TYPE = "\"type\"";
  public static final String VALUE = "\"value\"";
  public static final String INSTRS = "\"instrs\"";
  public static final String SIGNATURE = "\"signature\"";
  public static final String BOUND_NAME = "\"boundName\"";
  public static final String KEYWORD_INDICES = "\"keywordIndexes\"";
  public static final String CAPTURES = "\"captures\"";
  public static final String MODIFIERS = "\"modifiers\"";
  public static final String POSITIONAL_CNT = "\"positionalCount\"";
  public static final String KEYWORD_PARAMS = "\"keywordParams\"";
  public static final String HAS_VAR_PARAMS = "\"hasVarParams\"";
  public static final String OPCODE = "\"opcode\"";
  public static final String ARG = "\"arg\"";
  public static final String MODULE_LABEL = "\"moduleLabel\"";
  public static final String LINE = "\"line\"";
  public static final String COL = "\"col\"";
  public static final String ERR_JUMP = "\"errJmp\"";
  public static final String INDEX = "\"index\"";
  
  public static final String INT = "\"int\"";
  public static final String FLOAT = "\"float\"";
  public static final String BOOL = "\"bool\"";
  public static final String STRING = "\"string\"";
  public static final String CODE = "\"code\"";



  private IRWriter() {}
  
  public static boolean printCompiledFile(String destinationFolder, RuntimeModule compiledFile) {
    String module = writeCompiledFile(compiledFile);
    File newFile = new File(destinationFolder, compiledFile.getName()+".shrc");
    
    try {
      if (!newFile.createNewFile()) {
        return false;
      } 
      
      BufferedWriter writer = new BufferedWriter(new FileWriter(newFile));
      writer.write(module);
      writer.flush();
      writer.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }
  
  public static String writeCompiledFile(RuntimeModule compiledFile) {
    /*
     * .shrc structure.
     * _____________________________________________________________
     * | HEADER:  | 8-byte header representing interpreter version |
     * -------------------------------------------------------------
     * | Constant Pool: 8-byte header on pool size                 |
     * |  Entry description below                                  |
     * -------------------------------------------------------------
     * | Module instructions: 8-byte header on pool size           |
     * |  Each entry is: 9-bytes                                   |
     * -------------------------------------------------------------
     * 
     * Constant Pool component binary outline:
     *   Booleans: 0 (as a byte), then 0 (false, as a byte) or 1 (true, as a byte)
     *   Integer:  1 (as a byte), then the 8-bytes that represent that integer
     *   Float:    2 (as a byte), then the 8-bytes that represent that float
     *   String:   3 (as a byte), followed by 4-bytes representing byte count, followed by the bytes of this string
     *          - All strings are utf-8
     *   Code Object: 
     *              * 4 (as a byte)
     *              * 4-bytes representing instruction count, 
     *              * Function signature 
     *              * instructions of this code object. (see instruction encoding below)
     */
    
    //writeCompiledFile should be called AFTER ModuleFinder prepares it
    
    /*
     *  .shrc JSON structure
     *  
     *  {
     *    version: <current interpreter version>,
     *    pool: [
     *      {index: index, value: {type: 0, value: true}},  //type 0 is boolean
     *      {index: index, value: {type: 0, value: false}}, 
     *      
     *      {index: index, value: {type: 1, value: 126}},   //type 1 is an integer
     *      
     *      {index: index, value: {type: 2, value: 3.14}},  //type 2 is a float
     *      
     *      {index: index, value: {type: 3, value: "I'm a string"}},  //type 3 is a string
     *      
     *      {index: index, value: {type: 5, value:                   //type 5 is a code object
     *        {
     *           signature: {
     *                        modifiers: [enum orginal values of reserved words], 
     *                        positionalCount: <amount of positionals>,
     *                        keywordParams: [string keywords],
     *                        hasVarParams: true/false
     *                      },
     *           boundName: "boundName" or null,
     *           keywordIndexes: [[keyword1, 1], [keyword2, 2], ...],
     *           instrs: [ 
     *                     {opcode: <ordinal of OpCode enum>, arg: 10},
     *                     {opcode: <ordinal of OpCode enum>},
     *                     {opcode: <ordinal of OpCode enum>, arg: "some string"}
     *                     {opcode: <ordinal of OpCode enum>, arg: "some string", line: 2, col: 2},
     *                     ....
     *                   ],
     *           captures: [2,5,6,7]
     *        }
     *      }}
     *    ],
     *    instrs: [
     *              {opcode: <ordinal of OpCode enum>, arg: 10},
     *              {opcode: <ordinal of OpCode enum>},
     *              {opcode: <ordinal of OpCode enum>, arg: "some string"},
     *              {opcode: <ordinal of OpCode enum>, arg: "some string"},
     *              {opcode: <ordinal of OpCode enum>, arg: "some string", line: 2, col: 2, errJump: index},
     *              ....
     *            ],
     *    moduleLabel: "label"
     *  }
     */
    
    String jsonOutput = "{";
    
    //Put all contents in this region
    
    //put version:
    jsonOutput += VERSION+": "+SeaHorseInterpreter.VERSION+",";
    
    //put constant pool:
    jsonOutput += POOL+": "+writeConstantPool(compiledFile.getConstantMap())+",";
    
    //put moduleLabel
    //jsonOutput += MODULE_LABEL+": "+String.valueOf('"')+compiledFile.getModuleLabelStart()+String.valueOf('"')+",";
    
    //put instructions
    jsonOutput += INSTRS+": ["+Arrays.stream(compiledFile.getModuleCodeObject().getInstrs()).map(x -> writeInstr(x)).collect(Collectors.joining(","))+"]";
    
    jsonOutput += "}";
    return jsonOutput;   
  }
  
  public static String writeConstantPool(Map<Integer, RuntimeInstance> pool) {
    return "["+pool.entrySet().stream().map(x -> "{"+INDEX+": "+x.getKey()+" , "+VALUE+":"+writePoolComponent(x.getValue())+"}").collect(Collectors.joining(","))+"]";
  }
  
  public static String writePoolComponent(RuntimeInstance component) {
    String result = "{"+TYPE+": ";
    if (component instanceof RuntimeBool) {
      RuntimeBool comp = (RuntimeBool) component;
      result += BOOL+", "+VALUE+": "+comp.getValue();
    }
    else if (component instanceof RuntimeInteger) {
      RuntimeInteger comp = (RuntimeInteger) component;
      result += INT+", "+VALUE+": "+comp.getValue();
    }
    else if (component instanceof RuntimeFloat) {
      RuntimeFloat comp = (RuntimeFloat) component;
      result += FLOAT+", "+VALUE+": "+comp.getValue();
    }
    else if (component instanceof RuntimeString) {
      RuntimeString comp = (RuntimeString) component;
      result += STRING+", "+VALUE+": "+String.valueOf('"')+comp.getValue()+String.valueOf('"');
    }
    else if (component instanceof RuntimeCodeObject) {
      result += CODE+", "+VALUE+": "+writeCodeObject((RuntimeCodeObject) component);
    }
    
    return result+"}";
  }
  
  public static String writeCodeObject(RuntimeCodeObject codeObject) {
    return "{" 
         + "  "+SIGNATURE+": "+writeSignature(codeObject.getSignature())+", "
         + "  "+BOUND_NAME+": "+String.valueOf('"')+codeObject.getBoundName()+String.valueOf('"')+", "
         + "  "+KEYWORD_INDICES+": ["+codeObject.getKeywordIndexes().entrySet().stream().map(x -> "["+x.getKey()+", "+x.getValue()+"]").collect(Collectors.joining(","))+"], "
         + "  "+INSTRS+": ["+Arrays.stream(codeObject.getInstrs()).map(x -> writeInstr(x)).collect(Collectors.joining(","))+"], "
         + "  "+CAPTURES+": "+Arrays.toString(codeObject.getCaptures())
         + "}";
  }
  
  public static String writeSignature(FunctionSignature signature) {
    return "{"
         //+   " "+MODIFIERS+": ["+signature.getModifiers().stream().map(x -> String.valueOf(x.ordinal())).collect(Collectors.joining(","))+"], "
         +   " "+POSITIONAL_CNT+": "+signature.getPositionalParamCount()+", "
         +   " "+KEYWORD_PARAMS+": ["+signature.getKeywordParams().stream().map(x -> String.valueOf('"')+x+String.valueOf('"')).collect(Collectors.joining(","))+"], "
         //+   " "+HAS_VAR_PARAMS+": "+signature.hasVariableParams()
         + "}";
  }
  
  public static String writeInstr(ContextualInstr contextualInstr) {    
    final Instruction instruction = contextualInstr.getInstr();
    
    String result = "{"+OPCODE+": "+instruction.getOpCode().ordinal();
    
    if (instruction instanceof ArgInstr) {
      ArgInstr instr = (ArgInstr) instruction;
      result += ", "+ARG+": "+instr.getArgument();
    }
    else if (instruction instanceof CommentInstr) {
      CommentInstr instr = (CommentInstr) instruction;
      result += ", "+ARG+": "+String.valueOf('"')+instr.getContent()+String.valueOf('"');
    }
    else if (instruction instanceof JumpInstr) {
      JumpInstr instr = (JumpInstr) instruction;
      result += ", "+ARG+": "+String.valueOf('"')+instr.getTargetLabel()+String.valueOf('"');
    }
    else if (instruction instanceof IndexedJumpInstr) {
      IndexedJumpInstr instr = (IndexedJumpInstr) instruction;
      result += ", "+ARG+": "+instr.getJumpIndex();
    }
    else if (instruction instanceof LabelInstr) {
      LabelInstr instr = (LabelInstr) instruction;
      result += ", "+ARG+": "+String.valueOf('"')+instr.getName()+String.valueOf('"');
    }
    else if (instruction instanceof LoadCellInstr) {
      LoadCellInstr instr = (LoadCellInstr) instruction;
      result += ", "+ARG+": "+instr.getIndex();
    }
    else if (instruction instanceof NoArgInstr) {
      //do nothing. There's no argument
    }
    else if (instruction instanceof StoreCellInstr) {
      StoreCellInstr instr = (StoreCellInstr) instruction;
      result += ", "+ARG+": "+instr.getIndex();
    }
    
    result += ", "+LINE+": "+instruction.getLine()+", "+COL+": "+instruction.getCol()+", "+ERR_JUMP+": "+contextualInstr.getExceptionJumpIndex()+"}";
    return result;
  }
}
