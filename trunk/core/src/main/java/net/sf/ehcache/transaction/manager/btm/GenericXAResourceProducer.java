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

package net.sf.ehcache.transaction.manager.btm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.ResourceConfigurationException;
import bitronix.tm.resource.ResourceObjectFactory;
import bitronix.tm.resource.ResourceRegistrar;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.utils.ClassLoaderUtils;
import bitronix.tm.utils.PropertyUtils;

import java.util.Iterator;
import java.util.Map;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.transaction.xa.XAResource;

/**
 * @author nelrahma
 */
public class GenericXAResourceProducer extends ResourceBean implements XAResourceProducer {

    private final static Logger LOG = LoggerFactory.getLogger(GenericXAResourceProducer.class);
    private static final long serialVersionUID = 1L;

    private XAResource xaResource;
    private GenericXAResourceHolder xaResourceHolder;
    private RecoveryXAResourceHolder recoveryXAResourceHolder;


    /**
     *
     */
    public GenericXAResourceProducer() {
        //
    }

    /**
     * Util for reflection based handling
     * @param uniqueName
     * @param resource
     */
    public synchronized static void registerXAResource(String uniqueName, XAResource resource) {
        GenericXAResourceProducer producer = new GenericXAResourceProducer();
        producer.setXAResource(resource);
        producer.setUniqueName(uniqueName);
        producer.init();
    }

    /**
     *
     * @param resource
     */
    public void setXAResource(XAResource resource) {
        this.xaResource = resource;
    }

    /* XAResourceProducer implementation */

    /**
     * Need to instantiate the XAResource.
     */
    public void init() {

//        if (xaResource != null && xaResourceHolder != null) {
//            return;
//        }
        try {
            xaResource = createXAResource(this);
            xaResourceHolder = (GenericXAResourceHolder)createPooledConnection(xaResource, this);
            ResourceRegistrar.register(this);
            XAResourceHolderState xaResourceHolderState = new XAResourceHolderState(xaResourceHolder, this);
            xaResourceHolder.setXAResourceHolderState(xaResourceHolderState);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ResourceConfigurationException("cannot create XAResources named " + getUniqueName(), e);
        }
    }

    /**
     * @throws RecoveryException
     */
    public XAResourceHolderState startRecovery() throws RecoveryException {
        if (xaResource == null && xaResourceHolder == null) {
            init();
        }
        recoveryXAResourceHolder = xaResourceHolder.createRecoveryXAResourceHolder();
        return new XAResourceHolderState(recoveryXAResourceHolder, this);
    }


    /**
     * @throws RecoveryException
     */
    public void endRecovery() throws RecoveryException {
//        if (xaResourceHolder == null)
//            return;
//        try {
//            if (recoveryXAResourceHolder != null) {
//                recoveryXAResourceHolder = null;
//            }
//            xaResourceHolder = null;
//        } catch (Exception ex) {
//            throw new RecoveryException("error ending recovery on " + this, ex);
//        }
    }

    /**
     * {@inheritDoc}
     */
    public void setFailed(boolean failed) {
        //
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
        ResourceRegistrar.unregister(this);
        xaResourceHolder = null;
        recoveryXAResourceHolder = null;
    }

    /**
     * {@inheritDoc}
     */
    public XAStatefulHolder createPooledConnection(Object xaFactory,
                                                   ResourceBean bean) throws Exception {
        if (xaResourceHolder == null) {
            xaResourceHolder = new GenericXAResourceHolder((XAResource)xaFactory, this);
        }
        return xaResourceHolder;
    }

    /**
     * {@inheritDoc}
     */
    public XAResourceHolder findXAResourceHolder(XAResource aXAResource) {
        return (xaResourceHolder.getXAResource() == aXAResource) ? xaResourceHolder : null;
    }


    /**
     * PoolingDataSource must alway have a unique name so this method
     * builds a reference to this object using the unique name as
     * {@link javax.naming.RefAddr}.
     *
     * @return a reference to this PoolingDataSource.
     */
    public Reference getReference() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("creating new JNDI reference of " + this);
        }
        return new Reference(GenericXAResourceProducer.class.getName(),
            new StringRefAddr("uniqueName", getUniqueName()),
            ResourceObjectFactory.class.getName(), null);
    }

    private XAResource createXAResource(ResourceBean bean) throws Exception {
        if (xaResource != null) {
            return xaResource;
        }
        String className = bean.getClassName();
        if (className == null) {
            throw new IllegalArgumentException("className cannot be null");
        }
        Class xaResourceClass = ClassLoaderUtils.loadClass(className);
        XAResource resource = (XAResource)xaResourceClass.newInstance();

        Iterator it = bean.getDriverProperties().entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry entry = (Map.Entry)it.next();
            String name = (String)entry.getKey();
            String value = (String)entry.getValue();
            LOG.debug("setting vendor property '{}' to '{}'", name, value);
            PropertyUtils.setProperty(xaResourceClass, name, value);
        }
        return resource;
    }
}