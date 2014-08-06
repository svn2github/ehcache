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
package net.sf.ehcache.transaction;

import net.sf.ehcache.Element;

import java.io.Serializable;

/**
 * A soft lock ID is used to uniquely identify a soft lock
 *
 * @author Ludovic Orban
 */
public final class SoftLockID implements Serializable {

    private static final int PRIME = 31;

    private final TransactionID transactionID;
    private final Object key;
    private final Element newElement;
    private final Element oldElement;

    /**
     * Create a new SoftLockID instance
     * @param transactionID the transaction ID
     * @param key the element's key this soft lock is going to protect
     * @param newElement the new element, can be null
     * @param oldElement the old element, can be null
     */
    public SoftLockID(TransactionID transactionID, Object key, Element newElement, Element oldElement) {
        this.transactionID = transactionID;
        this.key = key;
        this.newElement = newElement;
        this.oldElement = oldElement;
    }

    /**
     * Get the ID of the transaction under which this soft lock is operating
     * @return the TransactionID
     */
    public TransactionID getTransactionID() {
        return transactionID;
    }

    /**
     * Get the key of the element this soft lock is guarding
     * @return the key
     */
    public Object getKey() {
        return key;
    }

    /**
     * Get the Element with which this soft lock should be replaced by on commit.
     * @return the commit Element
     */
    public Element getNewElement() {
        return newElement;
    }

    /**
     * Get the Element with which this soft lock should be replaced by on rollback.
     * @return the rollback Element
     */
    public Element getOldElement() {
        return oldElement;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        int hashCode = PRIME;

        hashCode *= transactionID.hashCode();
        hashCode *= key.hashCode();

        return hashCode;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object) {
        if (object instanceof SoftLockID) {
            SoftLockID other = (SoftLockID) object;

            if (!transactionID.equals(other.transactionID)) {
                return false;
            }

            if (!key.equals(other.key)) {
                return false;
            }

            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Soft Lock ID [transactionID: " + transactionID + ", key: " + key + "]";
    }

}
