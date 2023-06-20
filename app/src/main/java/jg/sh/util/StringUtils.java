package jg.sh.util;

import java.util.Arrays;

public class StringUtils {
  
  public static String getBareFileName(String rawFileName) {
    int dotIndex = rawFileName.indexOf('.');
    if (dotIndex == -1) {
      return rawFileName;
    }
    else {
      return rawFileName.substring(0, dotIndex);
    }
  }
  
  public static String leftPadStr(Object value, char padChar, int maxLength) {
    String str = value.toString();
    if (str.toString().length() < maxLength) {
      char [] pad = new char[maxLength - str.length()];
      Arrays.fill(pad, padChar);
      return String.valueOf(pad) + str;
    }
    return str;
  }

  public static String [] wrap(String ... args) {
    return args;
  }
}
