package jg.sh.util;

import static org.junit.jupiter.api.Assertions.*;


import java.util.Stack;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CacheStackTests {
  
  private Stack<Integer> instance;

  @BeforeEach
  public void init() {
    instance = new CachedStack<>();
  }

  /**
   * Test of isEmpty method of class Stack.
   */
  @Test
  public void testIsEmptyTrue() {
      System.out.println("isEmpty");
      assertTrue( instance.isEmpty() );
  }
    
    /**
     * Test of isEmpty method of class Stack.
     */
    @Test
    public void testIsEmptyFalse() 
    {
        System.out.println("isEmpty");
        instance.push( 1 );
        assertFalse( instance.isEmpty() );
    }

    /**
     * Test of pop method, of class Stack.
     */
    @Test
    public void testPop() 
    {
        System.out.println("pop");
        instance.push( 1 );
        assertEquals( new Integer( 1 ), instance.pop() );
    }
    
    /**
     * Test of remove method of class Stack.
     */
    @Test
    public void testPopException() 
    {
        System.out.println("pop");
        Integer peek = instance.pop();
    }

    /**
     * Test of push method, of class Stack.
     */
    @Test
    public void testPush() 
    {
        System.out.println("push");
        instance.push( -17 );
        assertEquals( instance.peek(), new Integer( -17 ) );
    }

    /**
     * Test of peek method, of class Stack.
     */
    @Test
    public void testPeek() 
    {
        System.out.println("peek");
        instance.push( 1 );
        assertEquals( new Integer( 1 ), instance.peek() );
    }

}
