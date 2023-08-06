package jg.sh.runtime.loading;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;

import jg.sh.runtime.alloc.HeapAllocator;
import jg.sh.runtime.instrs.RuntimeInstruction;
import jg.sh.runtime.objects.RuntimeCodeObject;
import jg.sh.runtime.objects.RuntimeDataRecord;
import jg.sh.runtime.objects.RuntimeInstance;
import jg.sh.util.StringUtils;

public class IRReader {
  
  public static final Charset UTF8 = StandardCharsets.UTF_8;

  public static RuntimeModule loadFromSHRCFile(HeapAllocator allocator, File path) throws IOException,  IllegalArgumentException {
    final String moduleName = StringUtils.getBareFileName(path.getName());
    final Path shrcPath = path.toPath();
    final FileChannel channel = (FileChannel) Files.newByteChannel(shrcPath, EnumSet.of(StandardOpenOption.READ));
    final MappedByteBuffer buffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

    try {
      final long savedInterpreterVersion = buffer.getLong();
      final RuntimeInstance [] constantPool = readConstants(buffer, allocator);
      final RuntimeCodeObject moduleInstrs = readModuleInstrs(buffer, allocator);
      channel.close();
      return new RuntimeModule(moduleName, moduleInstrs, constantPool);
    } catch (Exception e) {
      channel.close();
      throw e;
    }
  }

  private static RuntimeCodeObject readModuleInstrs(MappedByteBuffer buffer, HeapAllocator allocator) throws IOException {
    return null;
  }

  private static RuntimeInstance [] readConstants(MappedByteBuffer buffer, HeapAllocator allocator) throws IOException, IllegalStateException {
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

  private static RuntimeCodeObject readCodeObject(MappedByteBuffer buffer, HeapAllocator allocator) throws IOException {
    return null;
  }

  private static RuntimeDataRecord readDataDef(MappedByteBuffer buffer, HeapAllocator allocator) throws IOException {
    return null;
  }
}
