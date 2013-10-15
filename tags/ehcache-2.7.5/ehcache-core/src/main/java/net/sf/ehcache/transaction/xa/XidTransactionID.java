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

import net.sf.ehcache.transaction.TransactionID;

import javax.transaction.xa.Xid;

/**
 * A special TransactionID using a XID internally
 *
 * @author Ludovic Orban
 */
public interface XidTransactionID extends TransactionID {

    /**
     * Get the XID of this transaction ID
     * @return the XID
     */
    Xid getXid();

    /**
     * Get the name of the associated Ehcache resource.
     *
     * @return the Ehcache resource name
     */
    String getCacheName();
}
