/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.ehcache.tests.xa;

import net.sf.ehcache.transaction.manager.TransactionManagerLookup;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import javax.transaction.TransactionManager;
import java.util.Properties;

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
