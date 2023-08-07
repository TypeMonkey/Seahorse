package jg.sh.runtime.loading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import jg.sh.common.FunctionSignature;
import jg.sh.common.Location;
import jg.sh.compile.instrs.OpCode;
import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.instrs.ArgInstruction;
import jg.sh.runtime.instrs.CommentInstruction;
import jg.sh.runtime.instrs.NoArgInstruction;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeDataRecord;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.util.StringUtils;

public class IRReader {
  
  public static final Charset UTF8 = StandardCharsets.UTF_8;

  public static RuntimeModule loadFromSHRCFile(HeapAllocator allocator, String moduleName, byte [] data) throws IOException,  IllegalArgumentException{
    final ByteBuffer buffer = ByteBuffer.wrap(data);

    try {
      final long savedInterpreterVersion = buffer.getLong();
      final RuntimeInstance [] constantPool = readConstants(buffer, allocator);
      final RuntimeCodeObject moduleInstrs = readModuleInstrs(moduleName, buffer, allocator);
      return new RuntimeModule(moduleName, moduleInstrs, constantPool);
    } catch (Exception e) {
      throw e;
    }
  }

  public static RuntimeModule loadFromSHRCFile(HeapAllocator allocator, File path) throws IOException,  IllegalArgumentException {
    final String moduleName = StringUtils.getBareFileName(path.getName());
    final Path shrcPath = path.toPath();
    final FileChannel channel = (FileChannel) Files.newByteChannel(shrcPath, EnumSet.of(StandardOpenOption.READ));
    final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

    try {
      final long savedInterpreterVersion = buffer.getLong();
      final RuntimeInstance [] constantPool = readConstants(buffer, allocator);
      final RuntimeCodeObject moduleInstrs = readModuleInstrs(moduleName, buffer, allocator);
      channel.close();
      return new RuntimeModule(moduleName, moduleInstrs, constantPool);
    } catch (Exception e) {
      channel.close();
      throw e;
    }
  }

  private static RuntimeCodeObject readModuleInstrs(String moduleName, ByteBuffer buffer, HeapAllocator allocator) throws IOException {
    final int modInstrAmnt = buffer.getInt();
    final RuntimeInstruction [] modInstrs = new RuntimeInstruction[modInstrAmnt];
    for (int i = 0; i < modInstrs.length; i++) {
      modInstrs[i] = readInstruction(buffer);
    }

    final String boundName = ModuleFinder.MODULE_START_LABEL_GEN.apply(moduleName);

    return allocator.allocateCodeObject(boundName, 
                                        FunctionSignature.NO_ARG, 
                                        Collections.emptyMap(), 
                                        -1, 
                                        -1, 
                                        modInstrs, 
                                        new int[0]);
  }

  private static RuntimeInstruction readInstruction(ByteBuffer buffer) {
    final byte ordinalValue = buffer.get();
    final OpCode opCode = OpCode.values()[ordinalValue];

    RuntimeInstruction instr = null;

    if (opCode == OpCode.LABEL || opCode == OpCode.COMMENT) {
      final int stringSize = buffer.getInt();
      final byte [] stringBytes = new byte [stringSize];

      buffer.get(stringBytes);

      final String value = new String(stringBytes, UTF8);
      final int exceptionJumpIndex = buffer.getInt();
      instr = new CommentInstruction(exceptionJumpIndex, value);
    }
    else if(OpCode.isANoArgInstr(opCode)) {
      final int exceptionJumpIndex = buffer.getInt();
      instr = new NoArgInstruction(opCode, exceptionJumpIndex);
    }
    else {
      final int argument = buffer.getInt();
      final int exceptionJumpIndex = buffer.getInt();
      instr = new ArgInstruction(opCode, argument, exceptionJumpIndex);
    }

    final int startLine = buffer.getInt();
    final int startColumn = buffer.getInt();
    final int endLine = buffer.getInt();
    final int endColumn = buffer.getInt();

    instr.setStart(new Location(startLine, startColumn));
    instr.setEnd(new Location(endLine, endColumn));
    return instr;
  }

  private static RuntimeInstance [] readConstants(ByteBuffer buffer, HeapAllocator allocator) throws IOException, IllegalStateException {
    final int poolSize = buffer.getInt();
    final RuntimeInstance [] pool = new RuntimeInstance[poolSize];

    for (int i = 0; i < pool.length; i++) {
      final byte componentSignifier = buffer.get();
      switch (componentSignifier) {
        case IRWriter.BOOL_COMP_SIG: {
          final byte value = buffer.get();
          pool[i] = allocator.allocateBool(value == 1);
          break;
        }
        case IRWriter.INT_COMP_SIG: {
          final long value = buffer.getLong();
          pool[i] = allocator.allocateInt(value);
          break;
        }
        case IRWriter.FLOAT_COMP_SIG: {
          final double value = buffer.getDouble();
          pool[i] = allocator.allocateFloat(value);
          break;
        }
        case IRWriter.STR_COMP_SIG: {
          final int byteCount = buffer.getInt();
          final byte [] arr = new byte[byteCount];
          buffer.get(arr);

          final String value = new String(arr, UTF8);
          pool[i] = allocator.allocateString(value);
          break;
        }
        case IRWriter.CODE_COMP_SIG: {
          pool[i] = readCodeObject(buffer, allocator);
          break;
        }
        case IRWriter.DATA_COMP_SIG: {
          pool[i] = readDataDef(buffer, allocator);
          break;
        }
        default:
          throw new IllegalStateException("Unknown component of code: "+Integer.valueOf(componentSignifier));
      }
    }

    return pool;
  }

  private static RuntimeCodeObject readCodeObject(ByteBuffer buffer, HeapAllocator allocator) throws IOException {
    final int nameSize = buffer.getInt();
    final byte [] nameBytes = new byte[nameSize];
    buffer.get(nameBytes);

    final String boundName = new String(nameBytes, UTF8);

    //Read signature
    final int positionalAmount = buffer.getInt();
    final int keywordAmount = buffer.getInt();

    final Map<String, Integer> keywordIndexes = new HashMap<>();

    for (int i = 0; i < keywordAmount; i++) {
      final int keywordSize = buffer.getInt();
      final byte [] keywordBytes = new byte[keywordSize];
      buffer.get(keywordBytes);
      final String keyword = new String(keywordBytes, UTF8); 

      final int index = buffer.getInt();
      keywordIndexes.put(keyword, index);
    }

    final int varArgIndex = buffer.getInt();
    final int varKeyArgIndex = buffer.getInt();

    final FunctionSignature signature = new FunctionSignature(positionalAmount, keywordIndexes.keySet(), varArgIndex != -1, varKeyArgIndex != -1);

    //Read capture array
    final int [] captures = new int[buffer.getInt()];
    for (int i = 0; i < captures.length; i++) {
      captures[i] = buffer.getInt();
    }

    //Read instructions
    final RuntimeInstruction [] instrs = new RuntimeInstruction[buffer.getInt()];
    for (int i = 0; i < instrs.length; i++) {
      instrs[i] = readInstruction(buffer);
    }

    return allocator.allocateCodeObject(boundName, 
                                        signature, 
                                        keywordIndexes, 
                                        varArgIndex, 
                                        varKeyArgIndex, 
                                        instrs, 
                                        captures);
  }

  private static RuntimeDataRecord readDataDef(ByteBuffer buffer, HeapAllocator allocator) throws IOException {
    final int nameSize = buffer.getInt();
    final byte [] nameBytes = new byte[nameSize];
    buffer.get(nameBytes);
    final String name = new String(nameBytes, UTF8); 

    final boolean isSealed = buffer.get() == 1;

    final int methodCount = buffer.get();
    final Map<String, RuntimeCodeObject> methods = new HashMap<>();
    for (int i = 0; i < methodCount; i++) {
      final RuntimeCodeObject codeObject = readCodeObject(buffer, allocator);
      methods.put(codeObject.getBoundName(), codeObject);
    }

    return allocator.allocateDataRecord(name, methods, isSealed);
  }

}
