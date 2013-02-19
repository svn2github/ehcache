/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.AbstractCacheWriter;
import net.sf.ehcache.writer.writebehind.WriteBehindManager;
import net.sf.ehcache.writer.writebehind.operations.SingleOperationType;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.ehcache.tests.AbstractWriteBehindClient;
import org.terracotta.toolkit.Toolkit;

import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

public class BatchWriteBehindTest extends AbstractCacheTestBase {
  private int       totalWriteCount  = 0;
  private int       totalDeleteCount = 0;
  public static int          ELEMENT_COUNT    = 1000;

  public BatchWriteBehindTest(TestConfig testConfig) {
    super("batch-writebehind-test.xml", testConfig, BatchWriteBehindTestClient.class);
  }
  
  public static class BatchWriteBehindTestClient extends AbstractWriteBehindClient {

    public BatchWriteBehindTestClient(String[] args) {
      super(args);
    }

    @Override
    public long getSleepBetweenWrites() {
      return 100L;
    }

    @Override
    public long getSleepBetweenDeletes() {
      return 100L;
    }

    public static void main(String[] args) {
      new BasicWriteBehindTestClient(args).run();
    }

    @Override
    protected void runTest(final Cache cache, Toolkit toolkit) throws Throwable {
      BatchCacheWriter cacheWriter = new BatchCacheWriter(this);
      cache.registerCacheWriter(cacheWriter);
      for (int i = 0; i < ELEMENT_COUNT; i++) {
        cache.putWithWriter(new Element("key" + i % 200, "value" + i)); // 200 different keys, write operation
        if (0 == i % 10) {
          cache.removeWithWriter("key" + i % 200 / 10); // 10 different keys, delete operation
        }
      }

      final WriteBehindManager wbManager = ((WriteBehindManager) cache.getWriterManager());
      System.out.println("write behind queue size " + wbManager.getQueueSize());
      final AtomicLong counter = new AtomicLong();
      final ScheduledExecutorService executor = Executors.newScheduledThreadPool(10);
      executor.scheduleAtFixedRate(new Runnable() {
        @Override
        public void run() {
          long count = counter.incrementAndGet();
          cache.putWithWriter(new Element("key-" + count, "value-" + count));
          System.out.println("executor write behind queue size " + wbManager.getQueueSize() + " counter " + count);
        }
      }, 500L, 1L, TimeUnit.MILLISECONDS);

      // done with put now shutdown cache manager
      // this call should wait write behind queue to get empty
      Thread.sleep(TimeUnit.SECONDS.toMillis(1L));
      System.out.println("calling cacheManager shutdown");
      cache.getCacheManager().shutdown();
      
      try {
        wbManager.getQueueSize();
        Assert.fail("should have failed because cacheManager.shutdown is called before");
      } catch (IllegalStateException e) {
        // expected exception
      }
    }
  }

  public static class BatchCacheWriter extends AbstractCacheWriter {
    private final AbstractWriteBehindClient writeBehindClient;
    private final ArrayList<Class>          operationList = new ArrayList<Class>();

    public ArrayList<Class> getOperationList() {
      return operationList;
    }

    public BatchCacheWriter(AbstractWriteBehindClient writeBehindClient) {
      this.writeBehindClient = writeBehindClient;
    }

    @Override
    public void write(Element element) throws CacheException {
      operationList.add(element.getClass());
      writeBehindClient.incrementWriteCount();
      System.err.println("[WriteBehindCacheWriter written " + writeBehindClient.getWriteCount() + " for "
                         + writeBehindClient.getClass().getName() + "]");
      try {
        Thread.sleep(writeBehindClient.getSleepBetweenWrites());
      } catch (InterruptedException e) {
        // no-op
      }
    }

    @Override
    public void writeAll(Collection<Element> elements) throws CacheException {
      for (Element element : elements) {
        write(element);
      }
    }

    @Override
    public void delete(CacheEntry entry) throws CacheException {
      operationList.add(entry.getClass());
      writeBehindClient.incrementDeleteCount();
      System.err.println("[WriteBehindCacheWriter deleted " + writeBehindClient.getDeleteCount() + " for "
                         + writeBehindClient.getClass().getName() + "]");
      try {
        Thread.sleep(writeBehindClient.getSleepBetweenDeletes());
      } catch (InterruptedException e) {
        // no-op
      }
    }

    @Override
    public void deleteAll(Collection<CacheEntry> entries) throws CacheException {
      for (CacheEntry entry : entries) {
        delete(entry);
      }
    }

    @Override
    public void throwAway(Element element, SingleOperationType operationType, RuntimeException e) {
      System.err.println("BatchCacheWriter throwAway " + operationType.getClass() + " error " + e);
    }
  }

  @Override
  protected void postClientVerification() {
    System.out.println("[Clients processed a total of " + totalWriteCount + " writes]");
    if (totalWriteCount < ELEMENT_COUNT) { throw new AssertionError(totalWriteCount); }

    System.out.println("[Clients processed a total of " + totalDeleteCount + " deletes]");
    if (totalDeleteCount < ELEMENT_COUNT / 10) { throw new AssertionError(totalDeleteCount); }
  }

  @Override
  protected void evaluateClientOutput(String clientName, int exitCode, File output) throws Throwable {
    super.evaluateClientOutput(clientName, exitCode, output);

    FileReader fr = null;
    BufferedReader reader = null;
    StringBuilder strBuilder = new StringBuilder();
    try {
      fr = new FileReader(output);
      reader = new BufferedReader(fr);
      String st = "";
      while ((st = reader.readLine()) != null) {
        strBuilder.append(st);
      }
    } catch (Exception e) {
      throw new AssertionError(e);
    } finally {
      try {
        fr.close();
        reader.close();
      } catch (Exception e) {
        //
      }
    }

    // Detect the number of writes that have happened
    int writeCount = detectLargestCount(strBuilder.toString(),
                                        Pattern.compile("\\[WriteBehindCacheWriter written (\\d+) for (\\S+)\\]"));
    totalWriteCount += writeCount;
    System.out.println("[" + clientName + " processed " + writeCount + " writes]");

    // Detect the number of deletes that have happened
    int deleteCount = detectLargestCount(strBuilder.toString(),
                                         Pattern.compile("\\[WriteBehindCacheWriter deleted (\\d+) for (\\S+)\\]"));
    totalDeleteCount += deleteCount;
    System.out.println("[" + clientName + " processed " + deleteCount + " deletes]");
  }

  private int detectLargestCount(String clientOutput, Pattern pattern) {
    Matcher matcher = pattern.matcher(clientOutput);
    int count = 0;
    while (matcher.find()) {
      int parsedCount = Integer.parseInt(matcher.group(1));
      if (parsedCount > count) {
        count = parsedCount;
      }
    }
    return count;
  }
}
