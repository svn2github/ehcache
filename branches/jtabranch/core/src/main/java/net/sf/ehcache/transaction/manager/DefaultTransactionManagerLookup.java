/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;

/**
 * @author Alex Snaps
 */
public class DefaultTransactionManagerLookup implements TransactionManagerLookup {

    private transient TransactionManager transactionManager;
    private Set xaResources = new HashSet();
    private transient String vendor;
    private final Lock lock = new ReentrantLock();

    private final Selector[] transactionManagerSelectors = new Selector[] { new JndiSelector("JBoss", "java:/TransactionManager"),
            new FactorySelector("WebSphere 5.1", "com.ibm.ws.Transaction.TransactionManagerFactory"),
            new FactorySelector("Bitronix", "bitronix.tm.TransactionManagerServices"),
            new ClassSelector("Atomikos", "com.atomikos.icatch.jta.UserTransactionManager"), };

    /**
     *
     * @return
     */
    public TransactionManager getTransactionManager() {
        return getTransactionManager(null);
    }

    /**
     *
     * @param configuration
     * @return
     */
    public TransactionManager getTransactionManager(final CacheConfiguration configuration) {

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

    public void register(EhCacheXAResource resource) {
        if(vendor.equals("Bitronix")) {
            registerResourceWithBitronix(resource.getCacheName(), resource);
        }
    }

    private void registerResourceWithBitronix(String uniqueName, EhCacheXAResource resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            Class producerClass = cl.loadClass("bitronix.tm.resource.generic.GenericXAResourceProducer");
            Class[] signature = new Class[] { String.class, XAResource.class };
            Object[] args = new Object[] { uniqueName, resource };
            Method method = producerClass.getMethod("registerXAResource", signature);
            method.invoke(null, args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 
     * @return
     */
    public String getVendor() {
        return vendor;
    }

    private void lookupTransactionManager() {
        try {
            InitialContext context = new InitialContext();
            for (Selector selector : transactionManagerSelectors) {
                this.transactionManager = selector.lookup(context);
                if (this.transactionManager != null) {
                    this.vendor = selector.getVendor();
                    return;
                }
            }
        } catch (NamingException e) {
            //
        }
    }

    /**
     *
     */
    private abstract static class Selector {

        private final String vendor;

        protected Selector(final String vendor) {
            this.vendor = vendor;
        }

        public String getVendor() {
            return vendor;
        }

        protected abstract TransactionManager lookup(InitialContext initialContext);
    }

    /**
     * 
     */
    private static final class JndiSelector extends Selector {

        private final String jndiName;

        private JndiSelector(final String vendor, final String jndiName) {
            super(vendor);
            this.jndiName = jndiName;
        }

        public String getJndiName() {
            return jndiName;
        }

        @Override
        protected TransactionManager lookup(InitialContext initialContext) {
            Object jndiObject;
            try {
                jndiObject = initialContext.lookup(getJndiName());
                if (jndiObject instanceof TransactionManager) {
                    return (TransactionManager) jndiObject;
                }
            } catch (NamingException e) {
                //
            }
            return null;
        }
    }

    /**
     *
     */
    private static final class FactorySelector extends Selector {

        private final String factoryClassName;

        private FactorySelector(final String vendor, final String factoryClassName) {
            super(vendor);
            this.factoryClassName = factoryClassName;
        }

        @Override
        protected TransactionManager lookup(final InitialContext initialContext) {
            TransactionManager transactionManager = null;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            try {
                Class factoryClass = cl.loadClass(factoryClassName);
                Class[] signature = null;
                Object[] args = null;
                Method method = factoryClass.getMethod("getTransactionManager", signature);
                transactionManager = (TransactionManager) method.invoke(null, args);
            } catch (Exception e) {
                //
            }
            return transactionManager;
        }
    }

    /**
     *
     */
    private static final class ClassSelector extends Selector {

        private final String classname;

        private ClassSelector(final String vendor, final String classname) {
            super(vendor);
            this.classname = classname;
        }

        @Override
        protected TransactionManager lookup(final InitialContext initialContext) {
            TransactionManager transactionManager = null;
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl == null) {
                cl = ClassLoader.getSystemClassLoader();
            }
            try {
                Class txManagerClass = cl.loadClass(classname);
                transactionManager = (TransactionManager) txManagerClass.newInstance();
            } catch (Exception e) {
                //
            }
            return transactionManager;
        }
    }

}
