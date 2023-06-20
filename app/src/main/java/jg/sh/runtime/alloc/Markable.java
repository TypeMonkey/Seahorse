package jg.sh.runtime.alloc;

public interface Markable {
  
  public void gcMark(Cleaner allocator); 
  
  public void setGcFlag(int gcFlag);
  
  public int getGcFlag();
  
}
