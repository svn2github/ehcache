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

package net.sf.ehcache.transaction.xa;

import javax.transaction.Transaction;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * 
 * @author Nabib El-Rahman
 *
 */
public class XARequest {

    private final RequestType requestType;

    //todo: get rid of this field
    private final Transaction txn;

    private final Xid xid;
    private final int flags;
    private final boolean onePhase;
 
    /**
     * Constructor
     * 
     * @param requestType what request is this representing
     * @param txn the Transaction this request is being executed for
     * @param xid the Xid of the transaction this request is being executed for
     */
    public XARequest(RequestType requestType, Transaction txn, Xid xid) {
        this(requestType, txn, xid, XAResource.TMNOFLAGS, false);
    }
    
    /**
     * Constructor
     * 
     * @param requestType what request is this representing
     * @param txn the Transaction this request is being executed for
     * @param xid the Xid of the transaction this request is being executed for
     * @param flags the flags passed to the XAResource, when the request is made
     */
    public XARequest(RequestType requestType, Transaction txn, Xid xid, int flags) {
        this(requestType, txn, xid, flags, false);
    }
    
    /**
     * Constructor
     * 
     * @param requestType what request is this representing
     * @param txn the Transaction this request is being executed for
     * @param xid the Xid of the transaction this request is being executed for
     * @param flags the flags passed to the XAResource, when the request is made
     * @param onePhase whether this is a single phase commit
     */
    public XARequest(RequestType requestType, Transaction txn, Xid xid, int flags, boolean onePhase) {
        this.requestType = requestType;
        this.txn = txn;
        this.xid = xid;
        this.flags = flags;
        this.onePhase = onePhase;
    }
    
    /**
     * XA Requests types
     * 
     * @author Nabib El-Rahman
     *
     */
    public static enum RequestType {
        
        /**
         * prepare
         */
        PREPARE,
        
        /**
         * commit
         */
        COMMIT,
        
        /**
         * forget
         */
        FORGET,
        
        /**
         * rollback
         */
        ROLLBACK;
    }

    /**
     * 
     * @return the type of request
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * 
     * @return the Xid of the Transaction
     */
    public Xid getXid() {
        return xid;
    }

    /**
     * 
     * @return the flags passed to the XAResource when this request was made
     */
    public int getFlags() {
        return flags;
    }

    /**
     * 
     * @return true is one phase commit requested, otherwise false
     */
    public boolean isOnePhase() {
        return onePhase;
    }

    /**
     * get the current transaction
     *
     * @return the transaction for this request
     */
    public Transaction getTransaction() {
        return txn;
    }


}
