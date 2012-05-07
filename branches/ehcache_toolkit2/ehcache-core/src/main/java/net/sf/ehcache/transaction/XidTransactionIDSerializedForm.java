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
package net.sf.ehcache.transaction;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.transaction.xa.SerializableXid;

import javax.transaction.xa.Xid;
import java.io.Serializable;

/**
 * A replacement serialized form for XID transaction IDs. It can be used by transaction ID factories
 * to create XID-based IDs that serialize to this form (using writeReplace()) if they don't want or
 * cannot provide directly serializable IDs.
 * <p/>
 * During deserialization, objects of this class will be replaced by the result of the
 * CacheManager.restoreXidTransactionID() call.
 *
 * @author Ludovic Orban
 */
public class XidTransactionIDSerializedForm implements Serializable {
    private final String cacheManagerName;
    private final Xid xid;
    private final String decision;

    /**
     * Constructor
     *
     * @param cacheManagerName the name of the cache manager which contains the factory
     *                         that created the original XidTransactionID
     * @param xid the XidTransactionID's XID
     * @param decision the XidTransactionID's decision
     */
    public XidTransactionIDSerializedForm(String cacheManagerName, Xid xid, String decision) {
        this.cacheManagerName = cacheManagerName;
        this.xid = new SerializableXid(xid);
        this.decision = decision;
    }

    /**
     * Get the name of the cache manager which contains the factory that created the
     * original XidTransactionID
     *
     * @return the cache manager name
     */
    public String getCacheManagerName() {
        return cacheManagerName;
    }

    /**
     * Get the original XidTransactionID's XID
     *
     * @return the original XidTransactionID's XID
     */
    public Xid getXid() {
        return xid;
    }

    /**
     * Get the original XidTransactionID's decision
     * @return the original XidTransactionID's decision
     */
    public String getDecision() {
        return decision;
    }

    private Object readResolve() {
        CacheManager cacheManager = CacheManager.getCacheManager(cacheManagerName);
        if (cacheManager == null) {
            throw new TransactionException("unable to restore XID transaction ID from " + cacheManagerName);
        }
        return cacheManager.getOrCreateTransactionIDFactory().restoreXidTransactionID(this);
    }

}
