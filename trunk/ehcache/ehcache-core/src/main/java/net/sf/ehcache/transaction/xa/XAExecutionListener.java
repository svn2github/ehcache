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

package net.sf.ehcache.transaction.xa;

/**
 * Listener interface which provides callback hooks for listening to the 2PC lifecycle
 *
 * @author Ludovic Orban
 */
public interface XAExecutionListener {

    /**
     * Called when the resource is about to prepare
     * @param xaResource the XAResource about to prepare
     */
    void beforePrepare(EhcacheXAResource xaResource);

    /**
     * Called when the resource committed or rolled back
     * @param xaResource the XAResource which committed or rolled back
     */
    void afterCommitOrRollback(EhcacheXAResource xaResource);

}
