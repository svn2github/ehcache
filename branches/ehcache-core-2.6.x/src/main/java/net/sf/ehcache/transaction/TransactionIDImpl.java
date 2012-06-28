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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A transaction ID implementation with uniqueness across a single JVM
 *
 * @author Ludovic Orban
 */
public class TransactionIDImpl implements TransactionID {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private final int id;

    /**
     * Create a new TransactionIDImpl instance
     */
    public TransactionIDImpl() {
        this.id = ID_GENERATOR.getAndIncrement();
    }

    /**
     * Create a new TransactionIDImpl instance from an existing one
     * @param transactionId the transaction Id to copy
     */
    protected TransactionIDImpl(TransactionIDImpl transactionId) {
        TransactionIDImpl txIdImpl = transactionId;
        this.id = txIdImpl.id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean equals(Object obj) {
        if (obj instanceof TransactionIDImpl) {
            TransactionIDImpl otherId = (TransactionIDImpl) obj;
            return id == otherId.id;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final int hashCode() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return Integer.toString(id);
    }
}
