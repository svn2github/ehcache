/**
 *  Copyright Terracotta, Inc.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.transaction.TransactionManager;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.transaction.manager.selector.AtomikosSelector;
import net.sf.ehcache.transaction.manager.selector.BitronixSelector;
import net.sf.ehcache.transaction.manager.selector.GenericJndiSelector;
import net.sf.ehcache.transaction.manager.selector.GlassfishSelector;
import net.sf.ehcache.transaction.manager.selector.JndiSelector;
import net.sf.ehcache.transaction.manager.selector.NullSelector;
import net.sf.ehcache.transaction.manager.selector.Selector;
import net.sf.ehcache.transaction.manager.selector.WeblogicSelector;
import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link TransactionManagerLookup} implementation, that will be used by an {@link net.sf.ehcache.Cache#initialise() initializing}
 * Cache should the user have not specified otherwise.
 * <p>
 * This implementation will:
 * <ol>
 * <li>lookup a TransactionManager under java:/TransactionManager, this location can be overridden;
 * <li>if it failed, lookup for a Glassfish transaction manager;
 * <li>if it failed, lookup for a Weblogic transaction manager;
 * <li>if it failed, look for a Bitronix TransactionManager;
 * <li>and if it failed, finally an Atomikos one.
 * </ol>
 *
 * To specify under what specific name the TransactionManager is to be found, you can provide a jndiName property using
 * {@link #setProperties(java.util.Properties)}. That can be set in the CacheManager's configuration file.
 *
 * The first TransactionManager instance is then kept and returned on each {@link #getTransactionManager()} call
 *
 * @author Alex Snaps
 */
public class DefaultTransactionManagerLookup implements TransactionManagerLookup {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultTransactionManagerLookup.class.getName());

    private final Lock lock = new ReentrantLock();
    private final List<EhcacheXAResource> uninitializedEhcacheXAResources = new ArrayList<EhcacheXAResource>();
    private volatile boolean initialized = false;
    private volatile Selector selector;

    private final JndiSelector defaultJndiSelector = new GenericJndiSelector();

    private final Selector[] transactionManagerSelectors = new Selector[] {defaultJndiSelector,
        new GlassfishSelector(),
        new WeblogicSelector(),
        new BitronixSelector(),
        new AtomikosSelector()
    };

    /**
     * {@inheritDoc}
     */
    public void init() {
        if (!initialized) {
            lock.lock();
            try {
                Iterator<EhcacheXAResource> iterator = uninitializedEhcacheXAResources.iterator();
                while (iterator.hasNext()) {
                    if (getTransactionManager() == null) {
                        throw new CacheException("No Transaction Manager could be located, cannot initialize DefaultTransactionManagerLookup." +
                                                 " Caches which registered an XAResource: " + getUninitializedXAResourceCacheNames());
                    }
                    EhcacheXAResource resource = iterator.next();
                    selector.registerResource(resource, true);
                    iterator.remove();
                }
            } finally {
                lock.unlock();
            }
            initialized = true;
        }
    }

    private Set<String> getUninitializedXAResourceCacheNames() {
        Set<String> names = new HashSet<String>();
        for (EhcacheXAResource xar : uninitializedEhcacheXAResources) {
            names.add(xar.getCacheName());
        }
        return names;
    }

    /**
     * Lookup available txnManagers
     *
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager() {
        if (selector == null) {
            lock.lock();
            try {
                if (selector == null) {
                    lookupTransactionManager();
                }
            } finally {
                lock.unlock();
            }
        }
        return selector.getTransactionManager();
    }

    private void lookupTransactionManager() {
        for (Selector s : transactionManagerSelectors) {
            TransactionManager transactionManager = s.getTransactionManager();
            if (transactionManager != null) {
                this.selector = s;
                LOG.debug("Found TransactionManager for {}", s.getVendor());
                return;
            }
        }
        this.selector = new NullSelector();
        LOG.debug("Found no TransactionManager");
    }

    /**
     * {@inheritDoc}
     */
    public void register(EhcacheXAResource resource, boolean forRecovery) {
        if (initialized) {
            selector.registerResource(resource, forRecovery);
        } else {
            lock.lock();
            try {
                uninitializedEhcacheXAResources.add(resource);
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void unregister(EhcacheXAResource resource, boolean forRecovery) {
        if (initialized) {
            selector.unregisterResource(resource, forRecovery);
        } else {
            lock.lock();
            try {
                Iterator<EhcacheXAResource> iterator = uninitializedEhcacheXAResources.iterator();
                while (iterator.hasNext()) {
                    EhcacheXAResource uninitializedEhcacheXAResource = iterator.next();
                    if (uninitializedEhcacheXAResource == resource) {
                        iterator.remove();
                        break;
                    }
                }
            } finally {
                lock.unlock();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setProperties(Properties properties) {
        if (properties != null) {
            String jndiName = properties.getProperty("jndiName");
            if (jndiName != null) {
                defaultJndiSelector.setJndiName(jndiName);
            }
        }
    }

}
