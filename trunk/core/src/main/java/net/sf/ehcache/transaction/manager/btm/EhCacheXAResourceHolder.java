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

import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;

import javax.transaction.xa.XAResource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * EHCache implementation of BTM's XAResourceHolder
 *
 * @author lorban
 */
public class EhCacheXAResourceHolder extends AbstractXAResourceHolder {

    private final XAResource resource;
    private final ResourceBean bean;

    /**
     * Create a new EhCacheXAResourceHolder for a particular XAResource
     * @param resource the required XAResource
     * @param bean the required ResourceBean
     */
    public EhCacheXAResourceHolder(XAResource resource, ResourceBean bean) {
        this.resource = resource;
        this.bean = bean;
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return resource;
    }

    /**
     * Method is only there to remain compatible with pre-2.0.0 version of BTM
     * @return the ResourceBean associated with this Resource
     * @deprecated for compatibility with pre-2.0.0 version of BTM
     */
    @Deprecated
    public ResourceBean getResourceBean() {
        return bean;
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastReleaseDate() {
        throw new UnsupportedOperationException("EhCacheXAResourceHolder cannot be used with an XAPool");
    }

    /**
     * {@inheritDoc}
     */
    public List getXAResourceHolders() {
        List xaResourceHolders = new ArrayList(1);
        xaResourceHolders.add(this);
        return xaResourceHolders;
    }

}
