/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.agent.loader;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class JarManager {

  private static final long      DEFAULT_IDLE_TIME = 30 * 1000L;

  private final Map<String, Jar> jars              = new HashMap<String, Jar>();
  private Thread                 idleThread;
  private final long             idleTime;

  public JarManager() {
    this(DEFAULT_IDLE_TIME);
  }

  public JarManager(long idleTime) {
    this.idleTime = idleTime;
  }

  /**
   * package protected method used for tests
   */
  long getIdleTime() {
    return idleTime;
  }

  public synchronized Jar getOrCreate(String key, URL source) {
    if (source == null) throw new NullPointerException("null source");
    if (key == null) throw new NullPointerException("key source");

    Jar jar = jars.get(key);
    if (jar == null) {
      jar = new Jar(source, this);
      jars.put(key, jar);
    }
    return jar;
  }

  public synchronized Jar get(String key) {
    if (key == null) throw new NullPointerException("null source");
    return jars.get(key);
  }

  synchronized void jarOpened(Jar jar) {
    if (idleThread == null) {
      idleThread = new IdleThread(this, idleTime);
      idleThread.start();
    }
  }

  synchronized Collection<Jar> getJarsSnapshot() {
    return new ArrayList<Jar>(jars.values());
  }

  private static class IdleThread extends Thread {
    private final long       idle;
    private final JarManager manager;

    IdleThread(JarManager manager, long idle) {
      this.manager = manager;
      this.idle = idle;
      setName("JarManager idle thread");
      setDaemon(true);
    }

    @Override
    public void run() {
      final long sleep = Math.max(1000L, idle / 10);
      while (true) {
        try {
          sleep(sleep);
        } catch (InterruptedException e) {
          //
        }

        for (Jar jar : manager.getJarsSnapshot()) {
          jar.deflateIfIdle(idle);
        }
      }
    }
  }

}
