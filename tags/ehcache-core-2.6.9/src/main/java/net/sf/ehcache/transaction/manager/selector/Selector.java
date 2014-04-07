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
package net.sf.ehcache.transaction.manager.selector;

import net.sf.ehcache.transaction.xa.EhcacheXAResource;

import javax.transaction.TransactionManager;

/**
 * Abstract class which is used to do various things related to JTA transaction managers,
 * like looking them up, registering XAResources for recovery...
 *
 * @author Ludovic Orban
 */
public abstract class Selector {

    private final String vendor;
    private volatile TransactionManager transactionManager;

    /**
     * Constructor
     *
     * @param vendor an indicative transaction manager vendor name
     *               this selector is working with.
     */
    protected Selector(String vendor) {
        this.vendor = vendor;
    }

    /**
     * Get the vendor name
     * @return the vendor name
     */
    public String getVendor() {
        return vendor;
    }

    /**
     * Get the transaction manager, looking it up if necessary.
     * Once the transaction manager has been looked up, its reference is cached.
     * @return the transaction manager
     */
    public TransactionManager getTransactionManager() {
        if (transactionManager == null) {
            transactionManager = doLookup();
        }
        return transactionManager;
    }

    /**
     * Register an XAResource with the transaction manager.
     *
     * @param ehcacheXAResource the XAResource
     * @param forRecovery true if this XAResource is being registered purely for recovery purpose
     */
    public void registerResource(EhcacheXAResource ehcacheXAResource, boolean forRecovery) {
    }

    /**
     * Unregister an XAResource from the transaction manager.
     *
     * @param ehcacheXAResource the XAResource
     * @param forRecovery true if this XAResource was registered purely for recovery purpose
     */
    public void unregisterResource(EhcacheXAResource ehcacheXAResource, boolean forRecovery) {
    }

    /**
     * Lookup the transaction manager.
     *
     * @return the transaction manager, or null if it could not be looked up.
     */
    protected abstract TransactionManager doLookup();

}
