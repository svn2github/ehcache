package net.sf.ehcache.server.rest.resources;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.server.util.HttpUtil;

import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractRestTest {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractRestTest.class);

  @BeforeClass
  public static void checkServerIsUp() {
    long start = System.nanoTime();
    boolean interrupted = false;
    try {
      while (System.nanoTime() < start + TimeUnit.SECONDS.toNanos(300)) {
        try {
          HttpURLConnection result = HttpUtil.head("http://localhost:9090/ehcache/rest/");
          if (result.getResponseCode() == 200) {
            LOG.info("Found live REST server");
            return;
          }
        } catch (IOException e) {
          LOG.info("Exception while waiting for live server", e);
        }
        try {
          TimeUnit.SECONDS.sleep(10);
        } catch (InterruptedException e) {
          interrupted = true;
        }
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
    
    throw new AssertionError("No live server after 5 minutes of waiting");
  }
}
