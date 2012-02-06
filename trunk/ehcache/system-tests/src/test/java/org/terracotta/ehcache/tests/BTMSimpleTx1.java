/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

public class BTMSimpleTx1 extends SimpleTx1 {
  
  public BTMSimpleTx1(String[] args) {
    super(args);
    Configuration config = TransactionManagerServices.getConfiguration();
    config.setServerId("simpletx-1-" + Math.random());
    config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new BTMSimpleTx1(args).run();
  }
}
