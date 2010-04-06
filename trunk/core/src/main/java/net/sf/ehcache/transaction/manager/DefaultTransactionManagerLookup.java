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

import java.lang.reflect.Method;
import java.util.Properties;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import net.sf.ehcache.transaction.xa.EhcacheXAResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link TransactionManagerLookup} implementation, that will be used by an {@link net.sf.ehcache.Cache#initialise() initializing}
 * Cache should the user have not specified otherwise.<p>
 * This implementation will:
 * <ol>
 * <li>try lookup an {@link javax.naming.InitialContext};
 * <li>if successful, lookup a TransactionManager under java:/TransactionManager, this location can be overriden;
 * <li>if it failed, or couldn't find {@link javax.transaction.TransactionManager} instance, look for a WebSphere TransactionManager;
 * <li>then, a Bitronix;
 * <li>and finally an Atomikos one.
 * </ol>
 *
 * To specify under what specific name the TransactionManager is to be found, you can provide a jndiName property
 * using {@link #setProperties(java.util.Properties)}. That can be set in the CacheManager's configuration file.
 *
 * The first TransactionManager instance is then kept and returned on each {@link #getTransactionManager()} call
 *
 * @author Alex Snaps
 */
public class DefaultTransactionManagerLookup implements TransactionManagerLookup {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTransactionManagerLookup.class.getName());

    private       transient TransactionManager transactionManager;
    private       transient String             vendor;
    private       transient Properties         properties         = new Properties();
    private final           Lock               lock               = new ReentrantLock();

    private final JndiSelector defaultJndiSelector = new JndiSelector("genericJNDI", "java:/TransactionManager");

    private final Selector[] transactionManagerSelectors = new Selector[] {defaultJndiSelector,
            new FactorySelector("WebSphere 5.1", "com.ibm.ws.Transaction.TransactionManagerFactory"),
            new FactorySelector("Bitronix", "bitronix.tm.TransactionManagerServices"),
            new ClassSelector("Atomikos", "com.atomikos.icatch.jta.UserTransactionManager"), };

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
        if (vendor.equals("Bitronix")) {
            registerResourceWithBitronix(resource.getCacheName(), resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(EhcacheXAResource resource) {
        if (vendor.equals("Bitronix")) {
            unregisterResourceWithBitronix(resource.getCacheName(), resource);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
        parseProperties();
    }

    private void parseProperties() {
        if (this.properties != null) {
            String jndiName = this.properties.getProperty("jndiName");
            if (jndiName != null) {
                defaultJndiSelector.setJndiName(jndiName);
            }
        }
    }

    private void registerResourceWithBitronix(String uniqueName, EhcacheXAResource resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            Class producerClass = cl.loadClass("net.sf.ehcache.transaction.manager.btm.EhCacheXAResourceProducer");
            Class[] signature = new Class[] {String.class, XAResource.class};
            Object[] args = new Object[] {uniqueName, resource};
            Method method = producerClass.getMethod("registerXAResource", signature);
            method.invoke(null, args);
        } catch (Exception e) {
            LOG.error("unable to register resource of cache " + uniqueName + " with BTM", e);
        }
    }

    private void unregisterResourceWithBitronix(String uniqueName, EhcacheXAResource resource) {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (cl == null) {
            cl = ClassLoader.getSystemClassLoader();
        }
        try {
            Class producerClass = cl.loadClass("net.sf.ehcache.transaction.manager.btm.EhCacheXAResourceProducer");
            Class[] signature = new Class[] {String.class, XAResource.class};
            Object[] args = new Object[] {uniqueName, resource};
            Method method = producerClass.getMethod("unregisterXAResource", signature);
            method.invoke(null, args);
        } catch (Exception e) {
            LOG.error("unable to unregister resource of cache " + uniqueName + " with BTM", e);
        }
    }

    private void lookupTransactionManager() {
        InitialContext context = null;
        try {
            context = new InitialContext();
        } catch (NamingException e) {
            LOG.debug("Couldn't create an InitialContext", e);
        }

        for (Selector selector : transactionManagerSelectors) {
            this.transactionManager = selector.lookup(context);
            if (this.transactionManager != null) {
                this.vendor = selector.getVendor();
                LOG.debug("Found TransactionManager for {}", vendor);
                return;
            }
        }

        LOG.warn("No TransactionManager located!");
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

        private volatile String jndiName;

        private JndiSelector(final String vendor, final String jndiName) {
            super(vendor);
            this.jndiName = jndiName;
        }

        public String getJndiName() {
            return jndiName;
        }

        public void setJndiName(final String jndiName) {
            this.jndiName = jndiName;
        }

        @Override
        protected TransactionManager lookup(InitialContext initialContext) {

            if (initialContext == null) {
                return null;
            }

            Object jndiObject;
            try {
                jndiObject = initialContext.lookup(getJndiName());
                if (jndiObject instanceof TransactionManager) {
                    return (TransactionManager) jndiObject;
                }
            } catch (NamingException e) {
                LOG.debug("Couldn't locate TransactionManager for {} under {}", getVendor(), getJndiName());
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
