/**
 *  Copyright 2003-2010 Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.transaction.manager;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import net.sf.ehcache.util.ClassLoaderUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.java2d.pipe.OutlineTextRenderer;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * TransactionManager lookup and adapter for Webshpere's ExtendedJTATransaction
 *
 * @author Ludovic Orban
 */
public class WebsphereTransactionManagerLookup implements TransactionManagerLookup {

    private static final Logger LOG = LoggerFactory.getLogger(WebsphereTransactionManagerLookup.class.getName());

    private       transient TransactionManager transactionManager;
    private final           Lock               lock               = new ReentrantLock();

    /**
     * Lookup available txnManagers
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            lock.lock();
            try {
                if (transactionManager == null) {
                    lookupTransactionManager();
                }
            } finally {
                lock.unlock();
            }
        }
        return transactionManager;
    }

    /**
     * {@inheritDoc}
     */
    public void register(EhcacheXAResource resource) {
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(EhcacheXAResource resource) {
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(Properties properties) {
    }

    private void lookupTransactionManager() {
        InitialContext context = null;
        try {
            context = new InitialContext();
            Object extendedJTATransaction = context.lookup("java:comp/websphere/ExtendedJTATransaction");
            if (extendedJTATransaction != null) {
                transactionManager = new WebsphereTransactionManager(extendedJTATransaction);
            } else {
                LOG.warn("Websphere ExtendedJTATransaction could not be located!");
            }
        } catch (NamingException e) {
            LOG.warn("Couldn't create an InitialContext", e);
        } finally {
            if (context != null) {
                try {
                    context.close();
                } catch (NamingException e) {
                    LOG.debug("Couldn't close InitialContext", e);
                }
            }
        }
    }


    private static class WebsphereTransactionManager implements TransactionManager {
        private final Class synchronizationCallbackClass;
        private final Method registerSynchronizationMethod;
        private final Method getLocalIdMethod;
        private final Object extendedJTATransaction;

        private WebsphereTransactionManager(Object extendedJTATransaction) {
            this.extendedJTATransaction = extendedJTATransaction;
            try {
                synchronizationCallbackClass = ClassLoaderUtil.loadClass("com.ibm.websphere.jtaextensions.SynchronizationCallback");
                Class extendedJTATransactionClass = ClassLoaderUtil.loadClass("com.ibm.websphere.jtaextensions.ExtendedJTATransaction");
                registerSynchronizationMethod = extendedJTATransactionClass.getMethod("registerSynchronizationCallbackForCurrentTran",
                        synchronizationCallbackClass);
                getLocalIdMethod = extendedJTATransactionClass.getMethod("getLocalId");
            } catch (ClassNotFoundException e) {
                throw new CacheException(e);
            } catch (NoSuchMethodException e) {
                throw new CacheException(e);
            }
        }

        /**
         * {@inheritDoc}
         */
        public void begin() throws NotSupportedException, SystemException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void commit() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public int getStatus() throws SystemException {
            Transaction transaction = getTransaction();
            return transaction == null ? Status.STATUS_NO_TRANSACTION : transaction.getStatus();
        }

        /**
         * {@inheritDoc}
         */
        public Transaction getTransaction() throws SystemException {
            return new WebsphereTransaction();
        }

        /**
         * {@inheritDoc}
         */
        public void resume(Transaction tx) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void rollback() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void setRollbackOnly() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public void setTransactionTimeout(int timeout) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * {@inheritDoc}
         */
        public Transaction suspend() throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        private class WebsphereTransaction implements Transaction {

            /**
             * {@inheritDoc}
             */
            public void registerSynchronization(final Synchronization synchronization) throws RollbackException,
                    IllegalStateException, SystemException {

                final InvocationHandler invocationHandler = new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        if ("afterCompletion".equals(method.getName())) {
                            int status = args[2].equals(Boolean.TRUE) ? Status.STATUS_COMMITTED : Status.STATUS_UNKNOWN;
                            synchronization.afterCompletion(status);
                        } else if ("beforeCompletion".equals(method.getName())) {
                            synchronization.beforeCompletion();
                        } else if ("toString".equals(method.getName())) {
                            return synchronization.toString();
                        }
                        return null;
                    }
                };

                final Object synchronizationCallback = Proxy.newProxyInstance(this.getClass().getClassLoader(),
                        new Class[] { synchronizationCallbackClass }, invocationHandler);

                try {
                    registerSynchronizationMethod.invoke(extendedJTATransaction, synchronizationCallback);
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            }

            /**
             * {@inheritDoc}
             */
            public void commit() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             */
            public boolean delistResource(XAResource xar, int flags) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             */
            public boolean enlistResource(XAResource xar) throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             */
            public int getStatus() {
                try {
                    Object localId = getLocalIdMethod.invoke(extendedJTATransaction);
                    return new Integer(0).equals(localId) ? Status.STATUS_NO_TRANSACTION : Status.STATUS_ACTIVE;
                } catch (Exception e) {
                    throw new CacheException(e);
                }
            }

            private int getLocalId() {
                try {
                    return (Integer) getLocalIdMethod.invoke(extendedJTATransaction);
                } catch (IllegalAccessException e) {
                    throw new CacheException(e);
                } catch (InvocationTargetException e) {
                    throw new CacheException(e);
                }
            }

            @Override
            public int hashCode() {
                return getLocalId();
            }

            @Override
            public boolean equals(Object obj) {
                if (obj == null || !obj.getClass().equals(WebsphereTransaction.class)) {
                    return false;
                }

                WebsphereTransaction other = (WebsphereTransaction) obj;
                return getLocalId() == other.getLocalId();
            }

            /**
             * {@inheritDoc}
             */
            public void rollback() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }

            /**
             * {@inheritDoc}
             */
            public void setRollbackOnly() throws UnsupportedOperationException {
                throw new UnsupportedOperationException();
            }
        }
    }

}
