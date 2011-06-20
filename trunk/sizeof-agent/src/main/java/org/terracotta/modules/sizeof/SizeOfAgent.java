package org.terracotta.modules.sizeof;

import java.lang.instrument.Instrumentation;

/**
 * @author Alex Snaps
 */
public class SizeOfAgent {

  private static volatile Instrumentation instrumentation;

  public static void premain(String options, Instrumentation inst) {
    SizeOfAgent.instrumentation = inst;
  }

  public static void agentmain(String options, Instrumentation inst) {
    SizeOfAgent.instrumentation = inst;
  }

  public static long sizeOf(final Object obj) {
    return instrumentation.getObjectSize(obj);
  }

  public static boolean isAvailable() {
    return instrumentation != null;
  }
  
  private SizeOfAgent() {
    //not instantiable
  }
}
