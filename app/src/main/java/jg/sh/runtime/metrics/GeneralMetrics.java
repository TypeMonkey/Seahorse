package jg.sh.runtime.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jg.sh.compile.instrs.OpCode;

public class GeneralMetrics {

  public static enum Meaures {
    MOD_VAR_ATTR_LOOKUP, MOD_PUSH_OP, MOD_VAR_CASTING, MOD_VAR_LOAD, ATTR_LOOKUP, ATTR_LOOKUP_DIPATCH;
  }

  private final static Map<OpCode, List<Long>> opTimes = new EnumMap<>(OpCode.class);
  private final static Map<Meaures, List<Long>> otherMeasures = new EnumMap<>(Meaures.class);

  public static void addTimes(Meaures meaure, long time) {
    final List<Long> times = otherMeasures.getOrDefault(meaure, new ArrayList<>());
    times.add(time);
    otherMeasures.put(meaure, times);
  }

  public static void addTimes(OpCode op, long time) {
    final List<Long> times = opTimes.getOrDefault(op, new ArrayList<>());
    times.add(time);
    opTimes.put(op, times);
  }

  public static Map<OpCode, Double> getOpCodeAvgs() {
    final Map<OpCode, Double> avgs = new EnumMap<>(OpCode.class);

    opTimes.entrySet().forEach(entry -> {
      final double avg = entry.getValue().stream().mapToLong(Long::longValue).sum() / (double) entry.getValue().size();
      avgs.put(entry.getKey(), avg);
    });

    return avgs;
  }

  public static Map<Meaures, Double> getOtherAvgs() {
    final Map<Meaures, Double> avgs = new EnumMap<>(Meaures.class);

    otherMeasures.entrySet().forEach(entry -> {
      final double avg = entry.getValue().stream().mapToLong(Long::longValue).sum() / (double) entry.getValue().size();
      avgs.put(entry.getKey(), avg);
    });

    return avgs;
  }

  public static String statsAsStrings() {
    String x = "=====> OpCode Average Lengths (ns) <====="+System.lineSeparator();

    final ArrayList<Entry<OpCode, Double>> entryList = new ArrayList<>(getOpCodeAvgs().entrySet());
    Collections.sort(entryList, (o1, o2) -> (int) (o1.getValue() - o2.getValue()));
    
    for (Entry<OpCode, Double> entry : entryList) {
      x += " "+entry.getKey()+"   == "+entry.getValue()+" ns "+System.lineSeparator();
    }

    x += "=====> Other measures"+System.lineSeparator();

    final ArrayList<Entry<Meaures, Double>> measuresList = new ArrayList<>(getOtherAvgs().entrySet());
    Collections.sort(measuresList, (o1, o2) -> (int) (o1.getValue() - o2.getValue()));
    
    for (Entry<Meaures, Double> entry : measuresList) {
      x += " "+entry.getKey()+"   == "+entry.getValue()+" ns "+System.lineSeparator();
    }

    return x;
  }
}
