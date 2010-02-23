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
    private final Transaction txn;
    private final Xid xid;
    private final int flags;
    private final boolean onePhase;
 
    /**
     * Constructor
     * 
     * @param requestType
     * @param xid
     */
    public XARequest(RequestType requestType, Transaction txn, Xid xid) {
        this(requestType, txn, xid, XAResource.TMNOFLAGS, false);
    }
    
    /**
     * Constructor
     * 
     * @param requestType
     * @param xid
     * @param flags
     */
    public XARequest(RequestType requestType, Transaction txn, Xid xid, int flags) {
        this(requestType, txn, xid, flags, false);
    }
    
    /**
     * Constructor
     * 
     * @param requestType
     * @param xid
     * @param flags
     * @param onePhase
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
     * @return
     */
    public RequestType getRequestType() {
        return requestType;
    }

    /**
     * 
     * @return
     */
    public Xid getXid() {
        return xid;
    }

    /**
     * 
     * @return
     */
    public int getFlags() {
        return flags;
    }

    /**
     * 
     * @return
     */
    public boolean isOnePhase() {
        return onePhase;
    }

    /**
     * get the current transactions
     */
    public Transaction getTransaction() {
        return txn;
    }
    

}
