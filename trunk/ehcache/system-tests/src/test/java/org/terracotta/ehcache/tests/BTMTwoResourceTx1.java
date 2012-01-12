/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;


import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

public class BTMTwoResourceTx1 extends TwoResourceTx1 {
  
  public BTMTwoResourceTx1(String[] args) {
    super(args);
      Configuration config = TransactionManagerServices.getConfiguration();
      config.setServerId("tworesourcetx-1-" + Math.random());
      config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new BTMTwoResourceTx1(args).run();
  }
}
