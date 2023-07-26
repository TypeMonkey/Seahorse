package jg.sh.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jg.sh.common.Location;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool;

public class ConstantPoolTest {

  @Test
  public void testSimpleRemove() {
    final ConstantPool pool = new ConstantPool();

    final LoadInstr instr = new LoadInstr(Location.DUMMY, Location.DUMMY, OpCode.LOADC, -1);
    pool.addString("hello");
    pool.addInt(-548454145).linkInstr(instr);
    pool.addString("bye");

    assertEquals(3, pool.getPoolSize());
    assertEquals(1, pool.getIntegerConstants().size());
    assertEquals(1, instr.getIndex());
  }
}
