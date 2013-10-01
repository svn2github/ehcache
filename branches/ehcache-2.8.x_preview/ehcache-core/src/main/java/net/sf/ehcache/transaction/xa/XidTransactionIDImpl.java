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
package net.sf.ehcache.transaction.xa;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class XidTransactionIDImpl implements XidTransactionID {

    private final SerializableXid xid;
    private final String cacheName;

    /**
     * Constructor
     * @param xid a XID
     */
    public XidTransactionIDImpl(Xid xid, String cacheName) {
        this.xid = new SerializableXid(xid);
        this.cacheName = cacheName;
    }

    /**
     * {@inheritDoc}
     */
    public Xid getXid() {
        return xid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getCacheName() {
        return cacheName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof XidTransactionIDImpl) {
            XidTransactionIDImpl otherId = (XidTransactionIDImpl) obj;
            return xid.equals(otherId.xid);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return xid.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Unclustered " + xid;
    }
}
