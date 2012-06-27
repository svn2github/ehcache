/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import java.util.Properties;

import javax.transaction.TransactionManager;

public class DummyTransactionManagerLookup implements TransactionManagerLookup {

    private static DummyTransactionManager transactionManager;

    public TransactionManager getTransactionManager() {
        synchronized (DummyTransactionManagerLookup.class) {
            if (transactionManager == null) {
                transactionManager = new DummyTransactionManager();
            }
            return transactionManager;
        }
    }

    public void init() {
      //
    }

    public synchronized void register(EhcacheXAResource resource) {
      //
    }

    public synchronized void unregister(EhcacheXAResource resource) {
      //
    }

    public void setProperties(Properties properties) {
      //
    }
}
