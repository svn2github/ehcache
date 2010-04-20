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

package net.sf.ehcache.transaction.manager.btm;

import bitronix.tm.internal.BitronixRuntimeException;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.XAStatefulHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EHCache implementation of BTM's XAResourceProducer
 *
 * @author lorban
 */
public final class EhCacheXAResourceProducer extends ResourceBean implements XAResourceProducer {

    private static final Logger LOG = LoggerFactory.getLogger(EhCacheXAResourceProducer.class.getName());

    private static final Map PRODUCERS = new HashMap();

    private final List xaResourceHolders = new ArrayList();
    private RecoveryXAResourceHolder recoveryXAResourceHolder;


    private EhCacheXAResourceProducer() {
    }


    /**
     * Register an XAResource of a cache with BTM. The first time a XAResource is registered a new
     * EhCacheXAResourceProducer is created to hold it.
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static void registerXAResource(String uniqueName, XAResource xaResource) {
        synchronized (PRODUCERS) {
            EhCacheXAResourceProducer xaResourceProducer = (EhCacheXAResourceProducer) PRODUCERS.get(uniqueName);

            if (xaResourceProducer == null) {
                xaResourceProducer = new EhCacheXAResourceProducer();
                xaResourceProducer.setUniqueName(uniqueName);
                // the initial xaResource must be added before init() is called
                xaResourceProducer.addXAResource(xaResource);
                xaResourceProducer.init();

                PRODUCERS.put(uniqueName, xaResourceProducer);
            } else {
                xaResourceProducer.addXAResource(xaResource);
            }
        }
    }

    /**
     * Unregister an XAResource of a cache from BTM.
     * @param uniqueName the uniqueName of this XAResourceProducer, usually the cache's name
     * @param xaResource the XAResource to be registered
     */
    public static synchronized void unregisterXAResource(String uniqueName, XAResource xaResource) {
        synchronized (PRODUCERS) {
            EhCacheXAResourceProducer xaResourceProducer = (EhCacheXAResourceProducer) PRODUCERS.get(uniqueName);

            if (xaResourceProducer != null) {
                boolean found = xaResourceProducer.removeXAResource(xaResource);
                if (!found) {
                    LOG.error("no XAResource " + xaResource + " found in XAResourceProducer with name " + uniqueName);
                }
            } else {
                LOG.error("no XAResourceProducer registered with name " + uniqueName);
            }
        }
    }

    private static boolean isBtm13() {
        try {
            XAResourceHolder.class.getMethod("getResourceBean");
            return false;
        } catch (NoSuchMethodException e) {
            return true;
        }
    }


    private void addXAResource(XAResource xaResource) {
        synchronized (xaResourceHolders) {
            EhCacheXAResourceHolder xaResourceHolder = new EhCacheXAResourceHolder(xaResource, this);

            if (isBtm13()) {
                // BTM 1.3.x does not set the XAResourceHolderState itself
                XAResourceHolderState xaResourceHolderState = new XAResourceHolderState(xaResourceHolder, this);
                xaResourceHolder.setXAResourceHolderState(xaResourceHolderState);
            }

            xaResourceHolders.add(xaResourceHolder);
        }
    }

    private boolean removeXAResource(XAResource xaResource) {
        synchronized (xaResourceHolders) {
            for (int i = 0; i < xaResourceHolders.size(); i++) {
                EhCacheXAResourceHolder xaResourceHolder = (EhCacheXAResourceHolder) xaResourceHolders.get(i);
                if (xaResourceHolder.getXAResource() == xaResource) {
                    xaResourceHolders.remove(i);
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    public XAResourceHolderState startRecovery() throws RecoveryException {
        synchronized (xaResourceHolders) {
            if (recoveryXAResourceHolder != null) {
                throw new RecoveryException("recovery already in progress on " + this);
            }

            if (xaResourceHolders.isEmpty()) {
                throw new RecoveryException("no XAResource registered, recovery cannot be done on " + this);
            }

            recoveryXAResourceHolder = new RecoveryXAResourceHolder((XAResourceHolder) xaResourceHolders.get(0));
            return new XAResourceHolderState(recoveryXAResourceHolder, this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void endRecovery() throws RecoveryException {
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    public void setFailed(boolean failed) {
        // cache cannot fail as it's not connection oriented
    }

    /**
     * {@inheritDoc}
     */
    public XAResourceHolder findXAResourceHolder(XAResource xaResource) {
        synchronized (xaResourceHolders) {
            for (int i = 0; i < xaResourceHolders.size(); i++) {
                EhCacheXAResourceHolder xaResourceHolder = (EhCacheXAResourceHolder) xaResourceHolders.get(i);
                if (xaResource == xaResourceHolder.getXAResource()) {
                    return xaResourceHolder;
                }
            }

            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void init() {
        try {
            ResourceRegistrar.register(this);
        } catch (RecoveryException e) {
            throw new BitronixRuntimeException("error recovering " + this, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        synchronized (xaResourceHolders) {
            xaResourceHolders.clear();
            ResourceRegistrar.unregister(this);
        }
    }

    /**
     * {@inheritDoc}
     */
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception {
        throw new UnsupportedOperationException("EhCache is not connection-oriented");
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() throws NamingException {
        return new Reference(EhCacheXAResourceProducer.class.getName(),
                new StringRefAddr("uniqueName", getUniqueName()),
                ResourceObjectFactory.class.getName(), null);
    }

}
