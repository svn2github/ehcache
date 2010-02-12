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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.transaction.xa.XAResource;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.RecoveryXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;

/**
 * @author nelrahma
 */
public class GenericXAResourceHolder extends AbstractXAResourceHolder {

    private final XAResource resource;
    private final ResourceBean bean;

    /**
     *
     * @param resource the XAResource
     * @param bean the ResourceBean associated with the resource
     */
    public GenericXAResourceHolder(XAResource resource, ResourceBean bean) {
        this.resource = resource;
        this.bean = bean;
    }

    /**
     *
     * @return the Recovery XA Resource Holder
     */
    public RecoveryXAResourceHolder createRecoveryXAResourceHolder() {
        return new RecoveryXAResourceHolder(this);
    }

    /**
     * {@inheritDoc}
     */
    public XAResource getXAResource() {
        return resource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResourceHolderState getXAResourceHolderState() {
        XAResourceHolderState state = super.getXAResourceHolderState();
        if (state == null || state.isEnded()) {
            setXAResourceHolderState(new XAResourceHolderState(this, bean));
        }
        return super.getXAResourceHolderState();
    }

    /**
     * {@inheritDoc}
     */
    public void close() throws Exception {
        throw new UnsupportedOperationException(
            "GenericXAResourceHolder cannot be used with an XAPool implementation");
    }

    /**
     * {@inheritDoc}
     */
    public Object getConnectionHandle() throws Exception {
        throw new UnsupportedOperationException(
            "GenericXAResourceHolder cannot be used with an XAPool implementation");
    }

    /**
     * {@inheritDoc}
     */
    public Date getLastReleaseDate() {
        throw new UnsupportedOperationException(
            "GenericXAResourceHolder cannot be used with an XAPool implementation");
    }

    /**
     * {@inheritDoc}
     */
    public List getXAResourceHolders() {
        List xaResourceHolders = new ArrayList();
        xaResourceHolders.add(this);
        return xaResourceHolders;
    }

}
