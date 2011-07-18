package net.sf.ehcache.pool.sizeof;

import java.lang.instrument.Instrumentation;

/**
 * @author Alex Snaps
 */
public class SizeOfAgent {

  private static volatile Instrumentation instrumentation;
  private static final String INSTRUMENTATION_INSTANCE_SYSTEM_PROPERTY_NAME = "net.sf.ehcache.sizeof.agent.instrumentation";
  private static final String NO_INSTRUMENTATION_SYSTEM_PROPERTY_NAME = "net.sf.ehcache.sizeof.agent.noInstrumentationSystemProperty";

  public static void premain(String options, Instrumentation inst) {
    SizeOfAgent.instrumentation = inst;
    registerSystemProperty();
  }

  public static void agentmain(String options, Instrumentation inst) {
    SizeOfAgent.instrumentation = inst;
    registerSystemProperty();
  }

  private static void registerSystemProperty() {
    if (!Boolean.getBoolean(NO_INSTRUMENTATION_SYSTEM_PROPERTY_NAME)) {
      System.getProperties().put(INSTRUMENTATION_INSTANCE_SYSTEM_PROPERTY_NAME, instrumentation);
    }
  }

  public static Instrumentation getInstrumentation() {
    return instrumentation;
  }

  private SizeOfAgent() {
    //not instantiable
  }
}
