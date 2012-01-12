/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests;


import bitronix.tm.Configuration;
import bitronix.tm.TransactionManagerServices;

public class BTMTwoResourceTx2 extends TwoResourceTx2 {

  public BTMTwoResourceTx2(String[] args) {
    super(args);
      Configuration config = TransactionManagerServices.getConfiguration();
      config.setServerId("tworesourcetx-2-" + Math.random());
      config.setJournal("null");
  }
  
  public static void main(String[] args) {
    new BTMTwoResourceTx2(args).run();
  }
}
