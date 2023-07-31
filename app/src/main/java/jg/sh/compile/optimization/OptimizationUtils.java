package jg.sh.compile.optimization;

import jg.sh.compile.optimization.targets.BooleanTarget;
import jg.sh.compile.optimization.targets.FloatTarget;
import jg.sh.compile.optimization.targets.IntegerTarget;
import jg.sh.compile.optimization.targets.OptimizableTarget;
import jg.sh.compile.optimization.targets.StringTarget;

public final class OptimizationUtils {
  
  public static boolean isTarget(OptimizableTarget<?> target, long value) {
    return target instanceof IntegerTarget && ((IntegerTarget) target).getValue() == value;
  }

  public static boolean isTarget(OptimizableTarget<?> target, double value) {
    return target instanceof FloatTarget && ((FloatTarget) target).getValue() == value;
  }

  public static boolean isTarget(OptimizableTarget<?> target, String value) {
    return target instanceof StringTarget && ((StringTarget) target).getValue().equals(value);
  }

  public static boolean isTarget(OptimizableTarget<?> target, boolean value) {
    return target instanceof BooleanTarget && ((BooleanTarget) target).getValue() == value;
  }
}
