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
package net.sf.ehcache.transaction.manager;

import javax.transaction.TransactionManager;

import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import java.util.Properties;

/**
 * Interface to enable a XA transactional cache to access the JTA TransactionManager.
 * The implementing class can be configured in your xml file. It will then be instanciated by the Cache, during
 * {@link net.sf.ehcache.Cache#initialise() initialization}. It'll then have the properties injected, should any have been specified. And finally,
 * the TransactionManager will be queried for using #getTransactionManager.
 *
 * @author Alex Snaps
 */
public interface TransactionManagerLookup {

    /**
     * Switch the TransactionManagerLookup implementation to its initialized state.
     * All EhcacheXAResources registered before initialization are queued up internally
     * and are only registered with the transaction manager during initialization.
     */
    void init();

    /**
     * Lookup available txnManagers
     * @return TransactionManager
     */
    TransactionManager getTransactionManager();

    /**
     * execute txnManager specific code to register the XAResource for recovery.
     * @param resource the XAResource to register for recovery in the choosen TM.
     * @param forRecovery true if the XAResource is meant to be registered for recovery purpose only.
     */
    void register(EhcacheXAResource resource, boolean forRecovery);

    /**
     * execute txnManager specific code to unregister the XAResource for recovery.
     * @param resource the XAResource to register for recovery in the choosen TM.
     * @param forRecovery true if the XAResource is meant to be registered for recovery purpose only.
     */
    void unregister(EhcacheXAResource resource, boolean forRecovery);

    /**
     * Setter to the properties properties. This will be called right after the class has been instantiated.
     *
     * @param properties the properties parsed from the config file's
     *                   transactionManagerLookup tag's properties attribute
     */
    void setProperties(Properties properties);
}
