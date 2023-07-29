package jg.sh.compile;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import jg.sh.common.Location;
import jg.sh.compile.instrs.LoadInstr;
import jg.sh.compile.instrs.OpCode;
import jg.sh.compile.pool.ConstantPool;
import jg.sh.compile.pool.component.IntegerConstant;

public class ConstantPoolTest {

  @Test
  public void testSimpleRemove() {
    final ConstantPool pool = new ConstantPool();

    pool.addString("hello");
    final IntegerConstant integerConstant = pool.addInt(-548454145);
    pool.addString("bye");

    final LoadInstr instr = new LoadInstr(Location.DUMMY, Location.DUMMY, OpCode.LOADC, integerConstant.getIndex());

    assertEquals(3, pool.getPoolSize());
    assertEquals(1, pool.getIntegerConstants().size());
    assertEquals(1, instr.getArgument().getIndex());
  }
}
