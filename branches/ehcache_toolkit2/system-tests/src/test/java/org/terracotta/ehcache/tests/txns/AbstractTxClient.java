package org.terracotta.ehcache.tests.txns;

import org.terracotta.ehcache.tests.ClientBase;



public abstract class AbstractTxClient extends ClientBase {

  protected static final String BARRIER_NAME = "barrier1";
 
  public AbstractTxClient(String[] args) {
    super("test", args);
  }

}