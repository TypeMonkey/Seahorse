package jg.sh.runtime.loading;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.SeaHorseInterpreter;
import jg.sh.common.FunctionSignature;
import jg.sh.compile.instrs.ArgInstr;
import jg.sh.compile.instrs.CommentInstr;
import jg.sh.compile.instrs.Instruction;
import jg.sh.compile.instrs.LabelInstr;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.NoArgInstr;
import jg.sh.compile.instrs.StoreInstr;
import jg.sh.runtime.instrs.ArgInstruction;
import jg.sh.runtime.instrs.CommentInstruction;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeDataRecord;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.runtime.objects.literals.RuntimeBool;
import jg.sh.runtime.objects.literals.RuntimeFloat;
import jg.sh.runtime.objects.literals.RuntimeInteger;
import jg.sh.runtime.objects.literals.RuntimeString;

public final class IRWriter {
  
  private IRWriter() {}
  
  public static boolean printCompiledFile(String destinationFolder, RuntimeModule compiledFile) {
    File newFile = new File(destinationFolder, compiledFile.getName()+".shrc");
    
    try {
      if (!newFile.createNewFile()) {
        return false;
      } 
      
      FileOutputStream fileOutputStream = new FileOutputStream(newFile, false);
      fileOutputStream.write(encodeModule(compiledFile));
      fileOutputStream.flush();
      fileOutputStream.close();
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  public static byte [] encodeModule(RuntimeModule module) {
    /*
     * .shrc structure.
     * _____________________________________________________________
     * | HEADER:  | 8-byte header representing interpreter version |
     * -------------------------------------------------------------
     * | Constant Pool: 4-byte header on pool size                 |
     * |  Entry description below                                  |
     * -------------------------------------------------------------
     * | Module instructions: 4-byte header on instruction amount  |
     * -------------------------------------------------------------
     * 
     * Constant Pool component binary outline:
     *   Booleans: 0 (as a byte), then 0 (false, as a byte) or 1 (true, as a byte)
     *   Integer:  1 (as a byte), then the 8-bytes that represent that integer
     *   Float:    2 (as a byte), then the 8-bytes that represent that float
     *   String:   3 (as a byte), followed by 4-bytes representing byte count, followed by the bytes of this string
     *          - All strings are utf-8
     *   Code Object: 4 (as a byte)
     *        - 4-bytes representing instruction count, 
     *        - Function signature 
     *          -> Format: <positionalParamCount>
     *                     <4 bytes as keyword param length> <UTF-8 encoding of each keyword param>
     *                     <-1 to signify start of variatic arg>
     *                     <true or false for variabdic arg support. as byte>
     *        - Instruction count
     *        - Instructions of this code object. (see instruction encoding below)
     * 
     * 
     * Instruction encoding:
     *       <byte (OpCode ordinal value)>
     *       <4 bytes for integer argument>
     *       <4 bytes for exception jump>
     *       <4 bytes for start Line>
     *       <4 bytes for start column>
     *       <4 bytes for end line>
     *       <4 bytes for end column>
     */

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);
    try {
      ds.writeLong(SeaHorseInterpreter.VERSION);
      ds.write(encodeConstantPool(module.getConstantMap()));
      ds.writeInt(module.getModuleCodeObject().getInstrs().length);
      for (RuntimeInstruction instr : module.getModuleCodeObject().getInstrs()) {
        ds.write(encodeInstr(instr));
      }
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }
  
  public static byte [] encodeConstantPool(Map<Integer, RuntimeInstance> pool) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    /**
     * Constant Pool component binary outline:
     *   Booleans: 0 (as a byte), then 0 (false, as a byte) or 1 (true, as a byte)
     *   Integer:  1 (as a byte), then the 8-bytes that represent that integer
     *   Float:    2 (as a byte), then the 8-bytes that represent that float
     *   String:   3 (as a byte), followed by 4-bytes representing byte count, followed by the bytes of this string
     *          - All strings are utf-8
     *   Code Object: 4 (as a byte)
     *        - Bound Name: <4 bytes representing bound name length><bound name as UTF-8 bytes>
     *        - Function signature 
     *          -> Format: <positionalParamCount>
     *                     <4 bytes as keyword param length> <UTF-8 encoding of each keyword param>
     *                     <-1 byte to signify start of variatic arg>
     *                     <true or false for variabdic arg support. as byte>
     *        - Instruction count (4 bytes)
     *        - Instructions of this code object. (see instruction encoding below)
     *   Data Records: 5 (as a byte)
     *        - Name: <4 bytes for length><name encoding as utf-8>
     *        - isSeald? <byte for true or false>
     *        - Method: 
     *           -> Format: <method count as 4 bytes>
     *                      <code object encoding length><code object encoding>
     */
    try {
      ds.writeInt(pool.size());
      for(Entry<Integer, RuntimeInstance> instance : pool.entrySet()) {
        ds.write(encodePoolComponent(instance.getValue()));
      }
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }
  
  public static byte [] encodePoolComponent(RuntimeInstance instance) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    /**
     * Constant Pool component binary outline:
     *   Booleans: 0 (as a byte), then 0 (false, as a byte) or 1 (true, as a byte)
     *   Integer:  1 (as a byte), then the 8-bytes that represent that integer
     *   Float:    2 (as a byte), then the 8-bytes that represent that float
     *   String:   3 (as a byte), followed by 4-bytes representing byte count, followed by the bytes of this string
     *          - All strings are utf-8
     *   Code Object: 4 (as a byte)
     *        - Bound Name: <4 bytes representing bound name length><bound name as UTF-8 bytes>
     *        - Function signature 
     *          -> Format: <positionalParamCount>
     *                     <4 bytes as keyword param length> <UTF-8 encoding of each keyword param>
     *                     <-1 byte to signify start of variatic arg>
     *                     <true or false for variabdic arg support. as byte>
     *        - Instruction count (4 bytes)
     *        - Instructions of this code object. (see instruction encoding below)
     *   Data Records: 5 (as a byte)
     *        - Name: <4 bytes for length><name encoding as utf-8>
     *        - isSealed? <byte for true or false>
     *        - Method: 
     *           -> Format: <method count as 4 bytes>
     *                      <code object encoding length><code object encoding>
     */
    try {
      if (instance instanceof RuntimeBool) {
        RuntimeBool comp = (RuntimeBool) instance;
        ds.writeByte(0);
        ds.writeBoolean(comp.getValue());
      }
      else if (instance instanceof RuntimeInteger) {
        RuntimeInteger comp = (RuntimeInteger) instance;
        ds.writeByte(1);
        ds.writeLong(comp.getValue());
      }
      else if (instance instanceof RuntimeFloat) {
        RuntimeFloat comp = (RuntimeFloat) instance;
        ds.writeByte(2);
        ds.writeDouble(comp.getValue());
      }
      else if (instance instanceof RuntimeString) {
        RuntimeString comp = (RuntimeString) instance;
        final byte [] bytes = comp.getValue().getBytes(StandardCharsets.UTF_8);
        ds.writeByte(3);
        ds.writeInt(bytes.length);
        ds.write(bytes);
      }
      else if (instance instanceof RuntimeCodeObject) {
        ds.writeByte(4);
        ds.write(encodeCodeObject((RuntimeCodeObject) instance));
      }
      else if (instance instanceof RuntimeDataRecord) {
        ds.writeByte(5);
        ds.write(encodeDateDef((RuntimeDataRecord) instance));
      }
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }

  public static byte[] encodeDateDef(RuntimeDataRecord dataRecord) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    try {
      //Write out datarecord name
      final byte [] bytes = dataRecord.getName().getBytes(StandardCharsets.UTF_8);
      ds.writeInt(bytes.length);
      ds.write(bytes);

      //is sealed?
      ds.writeBoolean(dataRecord.isSealed());

      //write out method count
      ds.writeInt(dataRecord.getMethods().size());

      dataRecord.getMethods()
                .values()
                .stream()
      .forEach(x -> {
        final RuntimeCodeObject method = (RuntimeCodeObject) x;
        final byte [] encoding = encodeCodeObject(method);
        try {
          ds.writeInt(encoding.length);
          ds.write(encoding);
        } catch (IOException e) {
          //Should never happen.
          throw new IllegalStateException(e);
        }
      });
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }

  public static byte[] encodeCodeObject(RuntimeCodeObject codeObject) {
    /*
     *      *   Code Object: 4 (as a byte)
     *        - Bound Name: <4 bytes representing bound name length><bound name as UTF-8 bytes>
     *        - Function signature 
     *          -> Format: <positionalParamCount>
     *                     <4 bytes as keyword param length> <UTF-8 encoding of each keyword param>
     *                     <-1 byte to signify start of variatic arg>
     *                     <true or false for variabdic arg support. as byte>
     *        - Instruction count (4 bytes)
     *        - Instructions of this code object. (see instruction encoding below)
     */
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    try {
      //4 marks that this is a code object
      ds.writeByte(4);

      //Bound name encoding
      final byte [] nameEncoding = codeObject.getBoundName().getBytes(StandardCharsets.UTF_8);
      ds.writeInt(nameEncoding.length);
      ds.write(nameEncoding);

      //place signature next
      final byte [] sigEncoding = encodeFuncSignature(codeObject.getSignature());
      ds.write(sigEncoding);

      //Place instructions
      final RuntimeInstruction [] instrs = codeObject.getInstrs();
      ds.writeInt(instrs.length);
      for (RuntimeInstruction contextualInstr : instrs) {
        ds.write(encodeInstr(contextualInstr));
      }

    } catch (IOException e) {
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }

  public static byte[] encodeFuncSignature(FunctionSignature signature) {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    /*
        -> Format: <positionalParamCount>
                  <4 bytes as keyword param length> <UTF-8 encoding of each keyword param>
                  <-1 byte to signify start of variatic arg>
                  <true or false for variabdic arg support. as byte>
     */

    try {
      ds.writeInt(signature.getPositionalParamCount());
      signature.getKeywordParams().stream().forEach(x -> {
        final byte [] nameAsBytes = x.getBytes(StandardCharsets.UTF_8);
        try {
          ds.writeInt(nameAsBytes.length);
          ds.write(nameAsBytes);
        } catch (IOException e) {
          //should never happen
          throw new IllegalStateException(e);
        }
      });
      ds.writeByte(-1);
      ds.writeBoolean(signature.hasVariableParams());
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }

  public static byte [] encodeInstr(RuntimeInstruction instr) {
    /*
     * Instruction encoding (for non-comment, parameterized and jump instructions):
       <byte (OpCode ordinal value)>
       <4 bytes for integer argument>
       <4 bytes for exception jump>
       <4 bytes for start Line>
       <4 bytes for start column>
       <4 bytes for end line>
       <4 bytes for end column>

      for no-arg instruction:
       <byte (OpCode ordinal value)>
       <4 bytes for exception jump>
       <4 bytes for start Line>
       <4 bytes for start column>
       <4 bytes for end line>
       <4 bytes for end column>

      Comment instruction encoding:
       <byte (OpCode ordinal value)>
       <4 bytes for comment length as UTF-8 byte count>
       <UTF 8 encoding of comment>
       <4 bytes for exception jump>
       <4 bytes for start Line>
       <4 bytes for start column>
       <4 bytes for end line>
       <4 bytes for end column>

      For label instructions:
       <byte (OpCode ordinal value)>
       <4 bytes for label length as UTF-8 byte count>
       <UTF 8 encoding of label>
       <4 bytes for exception jump>
       <4 bytes for start Line>
       <4 bytes for start column>
       <4 bytes for end line>
       <4 bytes for end column>
     */

    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final DataOutputStream ds = new DataOutputStream(outputStream);

    try {
      //Write out ordinal
      ds.writeByte(instr.getOpCode().ordinal());

      if (instr instanceof ArgInstruction) {
        final ArgInstruction aInstr = (ArgInstruction) instr;
        ds.writeInt(aInstr.getArgument());
      }
      else if (instr instanceof CommentInstruction) {
        CommentInstruction cInstr = (CommentInstruction) instr;
        final byte [] commentBytes = cInstr.getComment().getBytes(StandardCharsets.UTF_8);
        ds.writeInt(commentBytes.length);
        ds.write(commentBytes);
      }
      else {
        throw new IllegalArgumentException(instr.getClass()+" is not an encodable instruction.");
      }

      /*
       * write:
       * - exception jump index
       * - start line
       * - start column
       * - end line
       * - enc column
       */
      ds.writeInt(instr.getExceptionJumpIndex());
      ds.writeInt(instr.getStart().line);
      ds.writeInt(instr.getStart().column);
      ds.writeInt(instr.getEnd().line);
      ds.writeInt(instr.getEnd().column);
    } catch (IOException e) {
      //Should never happen.
      throw new IllegalStateException(e);
    }

    return outputStream.toByteArray();
  }
}