package jg.sh.runtime.loading;

import java.io.File;
import java.io.IOException;

import jg.sh.runtime.alloc.HeapAllocator;

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
    return null;
  }
}
