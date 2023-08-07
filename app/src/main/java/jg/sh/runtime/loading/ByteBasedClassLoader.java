package jg.sh.runtime.loading;

public class ByteBasedClassLoader extends ClassLoader {
  
  public Class<?> loadClass(byte [] bytes) {
    return defineClass(null, bytes, 0, bytes.length);
  }

}
