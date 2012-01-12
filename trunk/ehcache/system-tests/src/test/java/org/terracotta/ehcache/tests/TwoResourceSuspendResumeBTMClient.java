/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

public class TwoResourceSuspendResumeBTMClient extends TwoResourceSuspendResumeClient {

  public TwoResourceSuspendResumeBTMClient(String[] args) {
    super(args);
    Configuration config = TransactionManagerServices.getConfiguration();
    config.setServerId("twoRCSuspendResume-1-" + Math.random());
    config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new TwoResourceSuspendResumeBTMClient(args).run();
  }
}
