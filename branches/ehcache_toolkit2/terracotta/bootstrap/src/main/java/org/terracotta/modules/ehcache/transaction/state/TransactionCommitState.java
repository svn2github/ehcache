/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.state;

/**
 * @author Abhishek Sanoujam
 */
public enum TransactionCommitState {

  COMMITTED() {

    @Override
    public boolean isCommitted() {
      return true;
    }

  },
  NOT_COMMITTED() {

    @Override
    public boolean isCommitted() {
      return false;
    }

  };

  public abstract boolean isCommitted();
}
