/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.util;

import org.apache.derby.drda.NetworkServerControl;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.test.TestConfigObject;

import java.net.InetAddress;
import java.util.Arrays;

public class DerbyWrapper {
  private final String         workingDir;
  private final int            port;
  private LinkedJavaProcess    linkedProcess;
  private NetworkServerControl control;

  public DerbyWrapper(int port, String workDir) {
    this.port = port;
    this.workingDir = workDir;
  }

  public void start() throws Exception {
    linkedProcess = new LinkedJavaProcess(NetworkServerControl.class.getName(), Arrays.asList("start", "-h", "0.0.0.0",
                                                                                              "-p",
                                                                                              String.valueOf(port),
                                                                                              "-noSecurityManager"),
                                          Arrays.asList("-Dderby.system.home=" + workingDir));
    linkedProcess.setClasspath(System.getProperty("java.class.path"));
    linkedProcess.setMaxRuntime(TestConfigObject.getInstance().getJunitTimeoutInSeconds());
    linkedProcess.start();
    linkedProcess.mergeSTDOUT("DERBY - ");
    linkedProcess.mergeSTDERR("DERBY - ");
    control = new NetworkServerControl(InetAddress.getLocalHost(), port);
    for (int count = 0; count < 30; count++) {
      try {
        control.ping();
        System.out.println("Ping succeeded. Derby server is up and running");
        break;
      } catch (Exception e) {
        System.out.println("Ping failed: " + e.getMessage() + ". Retrying #" + count + "...");
      }
      Thread.sleep(1000L);
    }
  }

  public void stop() {
    try {
      control.shutdown();
      linkedProcess.destroy();
    } catch (Exception e) {
      // ignored
    }
  }

}
