/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.state;

/**
 * @author Abhishek Sanoujam
 */
public enum XATransactionDecision {

  IN_DOUBT, COMMIT, ROLLBACK;
}
