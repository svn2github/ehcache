/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.util;

import org.apache.derby.drda.NetworkServerControl;

import com.tc.lcp.LinkedJavaProcess;

import java.util.Arrays;

public class DerbyWrapper {
  private final String         workingDir;
  private final int            port;
  private LinkedJavaProcess    linkedProcess;

  public DerbyWrapper(int port, String workDir) {
    this.port = port;
    this.workingDir = workDir;
  }

  public void start() throws Exception {
    linkedProcess = new LinkedJavaProcess(NetworkServerControl.class.getName(), Arrays.asList("start", "-p",
                                                                                              String.valueOf(port),
                                                                                              "-noSecurityManager"),
                                          Arrays.asList("-Dderby.system.home=" + workingDir));
    linkedProcess.setClasspath(System.getProperty("java.class.path"));
    linkedProcess.start();
    linkedProcess.mergeSTDOUT("DERBY - ");
    linkedProcess.mergeSTDERR("DERBY - ");
    for (int count = 0; count < 30 && !ping(); count++) {
      Thread.sleep(1000L);
    }
  }

  private boolean ping() throws Exception {
    LinkedJavaProcess pingProcess = new LinkedJavaProcess(NetworkServerControl.class.getName(),
                                                          Arrays.asList("ping", "-p", String.valueOf(port)),
                                                          Arrays.asList("-Dderby.system.home=" + workingDir));
    pingProcess.setClasspath(System.getProperty("java.class.path"));
    pingProcess.start();
    pingProcess.mergeSTDOUT("PING-DERBY - ");
    pingProcess.mergeSTDERR("PING-DERBY - ");
    pingProcess.waitFor();
    return pingProcess.exitValue() == 0;
  }

  public void stop() {
    try {
      linkedProcess.destroy();
    } catch (Exception e) {
      // ignored
    }
  }

}
