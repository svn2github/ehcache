/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.writebehind;

import org.terracotta.ehcache.tests.AbstractCacheTestBase;
import org.terracotta.modules.ehcache.async.AsyncCoordinatorImpl;

import com.tc.l2.L2DebugLogging.LogLevel;
import com.tc.test.config.model.TestConfig;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DistributionDeadBucketWriteBehindTest extends AbstractCacheTestBase {
  public static final String DISTRIBUTION_BARRIER_NAME = "DistributionDeadBucketWriteBehindTestBarrier";
  public static final int    NODE_COUNT                = 4;
  private int totalWriteCount  = 0;
  private int totalDeleteCount = 0;

  public DistributionDeadBucketWriteBehindTest(TestConfig testConfig) {
    super("basic-writebehind-test.xml", testConfig);
    disableTest();
    testConfig.getClientConfig().setClientClasses(DeadBucketWriteBehindClient.class, NODE_COUNT);
    testConfig.getClientConfig().setParallelClients(true);
    configureTCLogging(AsyncCoordinatorImpl.class.getName(), LogLevel.DEBUG);
  }

  @Override
  protected void postClientVerification() {
    System.out.println("[Clients processed a total of " + totalWriteCount + " writes]");
    if (totalWriteCount < 2000 || totalWriteCount > 2003) { throw new AssertionError(totalWriteCount); }

    System.out.println("[Clients processed a total of " + totalDeleteCount + " deletes]");
    if (totalDeleteCount < 200 || totalDeleteCount > 203) { throw new AssertionError(totalDeleteCount); }
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
                                        Pattern.compile("\\[WriteBehindCacheWriter written (\\d+) for " + clientName
                                                        + "\\]"));
    totalWriteCount += writeCount;
    System.out.println("[" + clientName + " processed " + writeCount + " writes]");
    if (writeCount < 1 || writeCount > 1001) { throw new AssertionError(
                                                                        "dead nodes distribution is not uniform writeCount "
                                                                            + writeCount); }

    // Detect the number of deletes that have happened
    int deleteCount = detectLargestCount(strBuilder.toString(),
                                         Pattern.compile("\\[WriteBehindCacheWriter deleted (\\d+) for " + clientName
                                                         + "\\]"));
    totalDeleteCount += deleteCount;
    System.out.println("[" + clientName + " processed " + deleteCount + " deletes]");
    if (deleteCount < 1 || deleteCount > 101) { throw new AssertionError(
                                                                         "dead nodes distribution is not uniform deleteCount "
                                                                             + deleteCount); }
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
