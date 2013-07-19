package net.sf.ehcache.search.parser;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;

import net.sf.ehcache.search.parser.CustomParseException;
import net.sf.ehcache.search.parser.MValue;
import net.sf.ehcache.search.parser.MValue.MEnum;
import net.sf.ehcache.search.parser.MValue.MObject;

import org.junit.Test;

public class MValueTest {

  public static enum Foo {
    Bar, Baz
  };

  @Test
  public void testEnumValue() throws CustomParseException {
    {
      MEnum enumVal = new MValue.MEnum(null,Foo.class.getName(), "Bar");
      Object obj = enumVal.asJavaObject();
      Assert.assertEquals(Foo.Bar, obj);
    }
    {
      try {
        @SuppressWarnings("unused")
        MEnum enumVal = new MValue.MEnum(null,Foo.class.getName(), "Barr");
        Assert.fail();
      } catch (Exception e) {
      }
    }

  }

  @Test
  public void testClassValue() throws CustomParseException {
    {
      MObject objValue = new MValue.MObject(null,String.class.getName(), "Bar");
      Object obj = objValue.asJavaObject();
      Assert.assertEquals("Bar", obj);
    }
    {
      try {
        @SuppressWarnings("unused")
        MObject objValue = new MValue.MObject(null,Integer.class.getName(), "Bar");
        Assert.fail();
      } catch (Exception e) {
      }
    }
  }
  
  @Test
  public void testHexString() throws CustomParseException {
    long seed=System.currentTimeMillis();
    System.out.println(this.getClass().getName()+".testHexString() Random seed: "+seed);
    Random r=new Random(seed);
    byte[] seedArray=new byte[10];
    StringBuilder sb=new StringBuilder();
    for(int i=0;i<seedArray.length;i++) {
      seedArray[i]=(byte)(r.nextInt()&0xff);
      sb.append(String.format("%02x", seedArray[i]));
    }
    Assert.assertTrue(sb.length()==2*seedArray.length); // sanity
    MValue.MBinary binValue=new MValue.MBinary(null,sb.toString());
    Assert.assertTrue(Arrays.equals(seedArray,binValue.asJavaObject()));
  }
  
}
