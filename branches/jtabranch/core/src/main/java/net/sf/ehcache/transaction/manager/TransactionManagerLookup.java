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
package net.sf.ehcache.transaction.manager;

import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.transaction.xa.EhCacheXAResource;

import javax.transaction.TransactionManager;

/**
 * @author Alex Snaps
 */
public interface TransactionManagerLookup {

    /**
     *
     * @param configuration
     * @return
     */
    TransactionManager getTransactionManager(Configuration configuration);
    
    void register(EhCacheXAResource resource);
}
