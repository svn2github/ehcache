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

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A transaction ID implementation with uniqueness across a single JVM
 *
 * @author Ludovic Orban
 */
public final class TransactionIDImpl implements TransactionID {

    private static final AtomicInteger ID_GENERATOR = new AtomicInteger();

    private final String uuid;
    private final long creationTime;
    private final int id;
    private volatile boolean commit;

    /**
     * Create a new TransactionIDImpl instance
     *
     * @param uuid a UUID
     */
    public TransactionIDImpl(String uuid) {
        this.uuid = uuid;
        this.creationTime = System.currentTimeMillis();
        this.id = ID_GENERATOR.getAndIncrement();
    }

    /**
     * Re-create a TransactionIDImpl instance from its internal state
     *
     * @param uuid the uuid
     * @param creationTime the creation time
     * @param id the id
     * @param commit the commit flag
     */
    public TransactionIDImpl(String uuid, long creationTime, int id, boolean commit) {
        this.uuid = uuid;
        this.creationTime = creationTime;
        this.id = id;
        this.commit = commit;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isDecisionCommit() {
        return commit;
    }

    /**
     * {@inheritDoc}
     */
    public void markForCommit() {
        this.commit = true;
    }

    @Override
    public final boolean equals(Object obj) {
      if (obj instanceof TransactionIDImpl) {
        TransactionIDImpl otherId = (TransactionIDImpl) obj;
        return id == otherId.id &&
               uuid.equals(otherId.uuid) &&
               creationTime == otherId.creationTime;
      }
      return false;
    }

    @Override
    public final int hashCode() {
      return (((id + (int) creationTime) * 31) ^ uuid.hashCode());
    }

    @Override
    public String toString() {
      return id + ":" + creationTime + "@" + uuid + (commit ? " (marked for commit)" : "");
    }

    /**
     * Get the UUID of this transaction ID
     *
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * Get the creation time of this transaction ID
     *
     * @return the creation time
     */
    public long getCreationTime() {
        return creationTime;
    }

    /**
     * Get the ID of this transaction ID
     *
     * @return the ID
     */
    public int getId() {
        return id;
    }
}
