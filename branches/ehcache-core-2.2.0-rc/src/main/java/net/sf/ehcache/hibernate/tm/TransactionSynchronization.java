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
package net.sf.ehcache.hibernate.tm;

import javax.transaction.Status;
import javax.transaction.Synchronization;

/**
 * @author Alex Snaps
 */
public class TransactionSynchronization implements Synchronization {

    private final SyncTransaction transaction;

    /**
     * Synchronization on a Transaction
     * @param transaction the SyncTransaction to commit or rollback after the real Transaction finished
     */
    public TransactionSynchronization(final SyncTransaction transaction) {
        this.transaction = transaction;
    }

    /**
     * {@inheritDoc}
     */
    public void beforeCompletion() {
        transaction.beforeCompletion();
    }

    /**
     * {@inheritDoc}
     */
    public void afterCompletion(final int status) {
        switch (status) {
            case Status.STATUS_COMMITTED:
                try {
                    transaction.commit();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            case Status.STATUS_ROLLEDBACK:
                try {
                    transaction.rollback();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                break;
            default:
                throw new IllegalArgumentException();
        }
    }
}
