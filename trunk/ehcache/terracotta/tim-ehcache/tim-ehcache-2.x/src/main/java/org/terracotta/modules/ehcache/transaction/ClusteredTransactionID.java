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
package org.terracotta.modules.ehcache.transaction;

import net.sf.ehcache.transaction.TransactionID;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Ludovic Orban
 */
public class ClusteredTransactionID implements TransactionID {

  private static final AtomicInteger idGenerator = new AtomicInteger();

  private final String clusterUUID;
  private final long creationTime;
  private final int id;
  private volatile boolean commit;

  ClusteredTransactionID(String clusterUUID) {
    this.clusterUUID = clusterUUID;
    this.creationTime = System.currentTimeMillis();
    this.id = idGenerator.getAndIncrement();
  }

  // autolocked in config
  public synchronized boolean isDecisionCommit() {
    return commit;
  }

  // autolocked in config
  public synchronized void markForCommit() {
    this.commit = true;
  }

  @Override
  public final boolean equals(Object obj) {
    if (obj instanceof ClusteredTransactionID) {
      ClusteredTransactionID otherId = (ClusteredTransactionID) obj;
      return id == otherId.id &&
              clusterUUID.equals(otherId.clusterUUID) &&
              creationTime == otherId.creationTime;
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return (((id + (int) creationTime) * 31) ^ clusterUUID.hashCode());
  }

  @Override
  public String toString() {
    return id + ":" + creationTime + "@" + clusterUUID + (commit ? " (marked for commit)" : "");
  }

}
