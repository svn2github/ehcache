package org.terracotta.ehcache.tests;



public abstract class AbstractTxClient extends ClientBase {

  protected static final String BARRIER_NAME = "barrier1";
 
  public AbstractTxClient(String[] args) {
    super("test", args);
  }

}