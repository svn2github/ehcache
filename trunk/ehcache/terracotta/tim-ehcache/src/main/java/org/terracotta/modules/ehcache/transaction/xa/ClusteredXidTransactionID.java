/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.transaction.xa;

import net.sf.ehcache.transaction.XidTransactionIDSerializedForm;
import net.sf.ehcache.transaction.xa.XidTransactionID;

import javax.transaction.xa.Xid;

/**
 * @author Ludovic Orban
 */
public class ClusteredXidTransactionID implements XidTransactionID {

    private final Xid xid;
    private final String cacheManagerName;

    public ClusteredXidTransactionID(XidTransactionIDSerializedForm serializedForm) {
        this.xid = new XidClustered(serializedForm.getXid());
        this.cacheManagerName = serializedForm.getCacheManagerName();
    }

    public ClusteredXidTransactionID(Xid xid, String cacheManagerName) {
        this.cacheManagerName = cacheManagerName;
        this.xid = new XidClustered(xid);
    }

    @Override
    public Xid getXid() {
        return xid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof ClusteredXidTransactionID) {
            ClusteredXidTransactionID otherId = (ClusteredXidTransactionID) obj;
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
        return "Clustered [" + xid + "]";
    }

    private Object writeReplace() {
        return new XidTransactionIDSerializedForm(cacheManagerName, xid);
    }
}
