/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

public class SuspendResumeBTMClient extends SuspendResumeClient {

  public SuspendResumeBTMClient(String[] args) {
    super(args);
    Configuration config = TransactionManagerServices.getConfiguration();
    config.setServerId("suspendResumeTx-1-" + Math.random());
    config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new SuspendResumeBTMClient(args).run();
  }
}
