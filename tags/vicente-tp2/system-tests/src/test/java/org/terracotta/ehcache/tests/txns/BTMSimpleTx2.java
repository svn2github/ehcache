/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.txns;

import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;
  
public class BTMSimpleTx2 extends SimpleTx2 {

  public BTMSimpleTx2(String[] args) {
    super(args);
    Configuration config = TransactionManagerServices.getConfiguration();
    config.setServerId("simpletx-2-" + Math.random());
    config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new BTMSimpleTx2(args).run();
  }
}
