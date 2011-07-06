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
package net.sf.ehcache.transaction.xa;

import java.io.Serializable;
import java.util.Arrays;

import javax.transaction.xa.Xid;

/**
 * A serializable XID
 *
 * @author Ludovic Orban
 */
public class SerializableXid implements Xid, Serializable {

    /*
     * int-encoded "Ehca" string.
     */
    private static final int FORMAT_ID = 0x45686361;

    private final byte[] globalTransactionId;
    private final byte[] branchQualifier;

    /**
     * Create a SerializableXid, copying the GTRID and BQUAL of an existing XID
     *
     * @param xid a SerializableXid
     */
    public SerializableXid(Xid xid) {
        this.globalTransactionId = xid.getGlobalTransactionId();
        this.branchQualifier = xid.getBranchQualifier();
    }

    /**
     * {@inheritDoc}
     */
    public int getFormatId() {
        return FORMAT_ID;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getBranchQualifier() {
        return branchQualifier;
    }

    /**
     * {@inheritDoc}
     */
    public byte[] getGlobalTransactionId() {
        return globalTransactionId;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object obj) {
        if (!(obj instanceof SerializableXid)) {
            return false;
        }

        SerializableXid otherXid = (SerializableXid) obj;
        return FORMAT_ID == otherXid.getFormatId() &&
               Arrays.equals(globalTransactionId, otherXid.getGlobalTransactionId()) &&
               Arrays.equals(branchQualifier, otherXid.branchQualifier);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        int hashCode = FORMAT_ID;
        if (globalTransactionId != null) {
            hashCode += Arrays.hashCode(globalTransactionId);
        }
        if (branchQualifier != null) {
            hashCode += Arrays.hashCode(branchQualifier);
        }
        return hashCode;
    }

}
