package jg.sh.util;

import java.util.Arrays;
import java.util.EmptyStackException;
import java.util.Stack;

public class CachedStack<T> extends Stack<T>{
  
  /*
   * Stack visualization:
   * 
   * TOP    ->
   *            t0     
   *            t1
   *            t2 
   * BOTTOM -> ....
   */

  private T t0;
  private T t1;
  private T t2;

  private int cacheSize; //Should be 3 at most and 0 at least

  @Override
  public T peek() {
    return t0 == null ? (t1 == null ? (t2 == null ? innerPeek() : t2) : t1): t0;
  }

  private T innerPeek() {
    if(isEmpty()) {
      throw new EmptyStackException();
    }

    //System.out.println("  -===> "+cacheSize+" | "+super.size());
    return elementAt(super.size() - 1);
  }

  @Override
  public T push(T value) {
    if(t1 == null) {
      t1 = t0;
    }
    else if(t2 == null) {
      t2 = t1;
      t1 = t0;
    }
    else if(t0 != null){
      super.push(t2);
      t2 = t1;
      t1 = t0;
    }

    if (cacheSize < 3) {
      cacheSize++;
    }

    t0 = value;
    return t0;
  }

  @Override
  public T pop() {
    if(t0 != null) {
      final T temp = t0;
      t0 = null;
      cacheSize--;
      return temp;
    }
    else if(t1 != null) {
      final T temp = t1;
      t1 = null;
      cacheSize--;
      return temp;
    }
    else if(t2 != null) {
      final T temp = t2;
      t2 = null;   
      cacheSize--;
      return temp;
    }
    else {
      final T res = super.pop();

      //Pull up the top three from the inner stack
      t0 = pullFromStack();
      t1 = pullFromStack();
      t2 = pullFromStack();

      return res;
    }
  }

  private T pullFromStack() {
    if (super.isEmpty()) {
      cacheSize++;
      return super.pop();
    }
    return null;
  }

  @Override
  public boolean isEmpty() {
    return empty();
  }

  @Override
  public boolean empty() {
    return size() == 0;
  } 

  @Override
  public int size() {
    return cacheSize + super.size();
  }
}
