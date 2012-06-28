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
 * @author Ludovic Orban
 */
public class ExpiredXidTransactionIDImpl extends XidTransactionIDImpl {

    /**
     * Create an expired XidTransactionID from a non-expired copy
     * @param xidTransactionId the non-expired copy
     */
    public ExpiredXidTransactionIDImpl(XidTransactionID xidTransactionId) {
        super(xidTransactionId.getXid(), xidTransactionId.getCacheName());
    }

}
