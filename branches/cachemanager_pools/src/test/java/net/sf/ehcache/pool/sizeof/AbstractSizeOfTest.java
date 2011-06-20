package net.sf.ehcache.pool.sizeof;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;

abstract class AbstractSizeOfTest {

  protected static final boolean COMPRESSED_OOPS;
  static {
    String value = getVmOptionValue("UseCompressedOops");
    if (value == null) {
      System.err.println("Could not detect compressed-oops status assuming: false");
      COMPRESSED_OOPS = false;
    } else {
      COMPRESSED_OOPS = Boolean.valueOf(value);
    }
  }
  
  private static String getVmOptionValue(String name) {
    try {
      MBeanServer server = ManagementFactory.getPlatformMBeanServer();
      Object vmOption = server.invoke(ObjectName.getInstance("com.sun.management:type=HotSpotDiagnostic"), "getVMOption", new Object[] {name}, new String[] {"java.lang.String"});
      return (String) ((CompositeData) vmOption).get("value");
    } catch (Throwable t) {
      return null;
    }
  }
}
