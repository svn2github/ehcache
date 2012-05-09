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
package net.sf.ehcache.hibernate;

import net.sf.ehcache.transaction.manager.DefaultTransactionManagerLookup;
import org.hibernate.HibernateException;
import org.hibernate.transaction.TransactionManagerLookup;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import java.util.Properties;

/**
 * Hibernate TransactionManagerLookup which also is a Ehcache transaction manager lookup.
 *
 * @author Ludovic Orban
 */
public class EhcacheJtaTransactionManagerLookup extends DefaultTransactionManagerLookup implements TransactionManagerLookup {

    /**
     * Construct a new transaction manager lookup.
     */
    public EhcacheJtaTransactionManagerLookup() {
        // no-op
    }

    /**
     * {@inheritDoc}
     */
    public TransactionManager getTransactionManager(Properties properties) throws HibernateException {
        return getTransactionManager();
    }

    /**
     * {@inheritDoc}
     */
    public String getUserTransactionName() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Object getTransactionIdentifier(Transaction transaction) {
        return transaction;
    }
}
