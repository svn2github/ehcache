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

package net.sf.ehcache.transaction.xa;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import net.sf.ehcache.transaction.xa.XARequest.RequestType;

/**
 * Default implementation for TransactionXARequestProcessor.
 * 
 * This class ties an Xid to an Executor service. This is necessary so that
 * locking for 2pc by the same thread.
 * 
 * {@link net.sf.ehcache.transaction.xa.XARequestProcessor xaRequestProcessor}.
 *  
 * @author Nabib El-Rahman
 */
public class TransactionXARequestProcessor implements XARequestProcessor {
    
    private final ConcurrentMap<Xid, ExecutorService> executorMap = new ConcurrentHashMap<Xid, ExecutorService>();
    private EhcacheXAResourceImpl resourceImpl;
    
    /**
     * Constructor
     * 
     * @param resourceImpl The EhcacheXAResourceImpl instance this processor will perform against
     */
    public TransactionXARequestProcessor(EhcacheXAResourceImpl resourceImpl) {
        this.resourceImpl = resourceImpl;
    }

    /**
     * {@inheritDoc}
     */
    public int process(XARequest request) throws XAException {
        int returnFlags = XAResource.TMNOFLAGS;
        
        ExecutorService service = getOrCreateExecutorService(request.getXid());
        Future<XAResponse> future = service.submit(new XARequestCallable(resourceImpl, request));
      
        XAResponse xaResponse = null;
        try {
            xaResponse = future.get();
        } catch (InterruptedException e) {
            cleanupExecutorService(request.getXid());
            throw new EhcacheXAException(e.getMessage(), -1, e);
        } catch (ExecutionException e) {
            cleanupExecutorService(request.getXid());
            throw new EhcacheXAException(e.getMessage(), -1, e);
        }
        if (xaResponse.getXaException() != null) {
            cleanupExecutorService(request.getXid());
            throw new EhcacheXAException("XA request failed", xaResponse.getXaException().errorCode, xaResponse.getXaException());
        }
        
        if (request.getRequestType().equals(RequestType.COMMIT) || 
           request.getRequestType().equals(RequestType.ROLLBACK) ||
           request.getRequestType().equals(RequestType.FORGET)) {
            cleanupExecutorService(request.getXid());
        }

        return returnFlags;
    }
    
    /**
     * Gets the executor service for a Transaction, either by creating a new one if none exists, or returning the
     * existing one
     * @param xid The Xid of the Transaction
     * @return the ExecutorService for that Transaction
     */
    private ExecutorService getOrCreateExecutorService(Xid xid) {
        ExecutorService service = executorMap.get(xid);
        if (service == null) {
            service = Executors.newSingleThreadExecutor(new XARequestProcessThreadFactory(xid));
            executorMap.put(xid, service);
        }
        return service;
    }
    
    /**
     * Removes the ExecutorService from the map and shuts it down
     * @param xid The Xid of the Transaction
     */
    private void cleanupExecutorService(Xid xid) {
        ExecutorService service = executorMap.remove(xid);
        service.shutdown();
    }
    
    /**
     * The creates the XA Request process thread with the correct thread name
     * @author Nabib El-Rahman
     *
     */
    private static class XARequestProcessThreadFactory implements ThreadFactory {
        
        private final Xid xid;
        
        /**
         * Thread factory for xid
         * @param xid associated with thread name
         */
        public XARequestProcessThreadFactory(Xid xid) {
            this.xid = xid;
        }

        /**
         * return new correctly named thread
         */
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, "XA-Request Thread Xid [ " + xid + " ] ");
        }
        
    }
    
    /**
     * Class to furnish
     * @author Nabib El-Rahman
     *
     */
    private static class XARequestCallable implements Callable<XAResponse> {
        private final EhcacheXAResourceImpl resourceImpl;
        private final XARequest request;
        
        /**
         * Constructor
         * @param resourceImpl the EhcacheXAResourceImpl this Request will be used for
         * @param request the actual Request
         */
        public XARequestCallable(EhcacheXAResourceImpl resourceImpl, XARequest request) {
            this.resourceImpl = resourceImpl;
            this.request = request;
        }
             
        /**
         * 
         */
        public XAResponse call() throws Exception {
            int returnFlag = XAResource.TMNOFLAGS;
            XAException xaException = null;
            try {
            switch(request.getRequestType()) {
                
                case FORGET:
                    resourceImpl.forgetInternal(request.getXid());
                    break;
                    
                case PREPARE:
                    returnFlag = resourceImpl.prepareInternal(request.getXid());
                    break;
                    
                case ROLLBACK:
                    resourceImpl.rollbackInternal(request.getXid());
                    break;
                    
                case COMMIT:
                    resourceImpl.commitInternal(request.getXid(), request.isOnePhase());
                    break;
                
                default:
                    throw new XAException("Unknown enum type: " + request.getRequestType());
            }
            } catch (XAException xaE) {
                xaException = xaE;
            }
            
            return new XAResponse(returnFlag, xaException);
        }
        
    }
    
    /**
     * 
     * @author nelrahma
     *
     */
    private static class XAResponse {
        
        private final int flags;
        private final XAException xaException;
        
        /**
         * Constructor
         * @param flags flags returned by the actual call against the XAResource
         * @param xaException Exception thrown by the call, otherwise null
         */
        public XAResponse(int flags, XAException xaException) {
            this.flags = flags;
            this.xaException = xaException;
        }

        /**
         * Gets the flags returned by the actual call against the XAResource
         * @return the flags
         */
        public int getFlags() {
            return flags;
        }

        /**
         * Gets the Exception thrown by the actual call against the XAResource
         * @return the exception, null if none
         */
        public XAException getXaException() {
            return xaException;
        }   
        
    }

}
