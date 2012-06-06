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

package net.sf.ehcache.transaction.xa.processor;

import net.sf.ehcache.transaction.xa.EhcacheXAException;
import net.sf.ehcache.transaction.xa.EhcacheXAResourceImpl;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Default implementation for XARequestProcessor.
 *
 * This class ties an Xid to an Executor service. This is necessary so that
 * locking for 2pc by the same thread.
 *
 * {@link XARequestProcessor xaRequestProcessor}.
 *
 * @author Nabib El-Rahman
 */
public class XARequestProcessor {

    private static volatile XAThreadPool xaProcessorPool;

    private final ConcurrentMap<Xid, XAThreadPool.MultiRunner> executorMap =
            new ConcurrentHashMap<Xid, XAThreadPool.MultiRunner>();
    private final EhcacheXAResourceImpl resourceImpl;

    /**
     * Constructor
     *
     * @param resourceImpl The EhcacheXAResourceImpl instance this processor will perform against
     */
    public XARequestProcessor(EhcacheXAResourceImpl resourceImpl) {
        this.resourceImpl = resourceImpl;
        if (xaProcessorPool == null) {
            xaProcessorPool = new XAThreadPool();
        }
    }

    /**
     * Release resources shared by all XARequestProcessors
     */
    public static void shutdown() {
        if (xaProcessorPool != null) {
            xaProcessorPool.shutdown();
            xaProcessorPool = null;
        }
    }

    /**
     * Process a XARequest
     * @param request the XARequest
     * @return the XAResource response code
     * @throws XAException the XAException thrown by the XAResource
     */
    public int process(XARequest request) throws XAException {
        XAThreadPool.MultiRunner multiRunner = getOrCreateThread(request.getXid());

        XAResponse xaResponse;
        try {
            xaResponse = (XAResponse) multiRunner.execute(new XARequestCallable(resourceImpl, request, request.getXid()));
        } catch (InterruptedException e) {
            cleanupThread(request.getXid());
            throw new EhcacheXAException(e.getMessage(), XAException.XAER_RMERR, e);
        } catch (ExecutionException e) {
            cleanupThread(request.getXid());
            throw new EhcacheXAException(e.getMessage(), XAException.XAER_RMERR, e);
        }
        if (xaResponse.getXaException() != null) {
            cleanupThread(request.getXid());
            throw new EhcacheXAException("XA " + request.getRequestType().toString().toLowerCase() +
                    " request failed on [" + request.getXid() + "]", xaResponse.getXaException().errorCode,
                    xaResponse.getXaException());
        }

        if (request.getRequestType().equals(XARequest.RequestType.COMMIT) ||
            request.getRequestType().equals(XARequest.RequestType.ROLLBACK) ||
            request.getRequestType().equals(XARequest.RequestType.FORGET) ||
            (request.getRequestType().equals(XARequest.RequestType.PREPARE) && xaResponse.getFlags() == XAResource.XA_RDONLY)) {
            cleanupThread(request.getXid());
        }

        return xaResponse.getFlags();
    }

    /**
     * Gets the executor service for a Transaction, either by creating a new one if none exists, or returning the
     * existing one
     * @param xid The Xid of the Transaction
     * @return the ExecutorService for that Transaction
     */
    private XAThreadPool.MultiRunner getOrCreateThread(Xid xid) {
        XAThreadPool.MultiRunner service = executorMap.get(xid);
        if (service == null) {
            service = xaProcessorPool.getMultiRunner();
            executorMap.put(xid, service);
        }
        return service;
    }

    /**
     * Removes the ExecutorService from the map and shuts it down
     * @param xid The Xid of the Transaction
     */
    private void cleanupThread(Xid xid) {
        XAThreadPool.MultiRunner service = executorMap.remove(xid);
        service.release();
    }

    /**
     * Class to furnish
     * @author Nabib El-Rahman
     *
     */
    private static class XARequestCallable implements Callable<XAResponse> {
        private final EhcacheXAResourceImpl resourceImpl;
        private final XARequest request;
        private final Xid xid;

        /**
         * Constructor
         * @param resourceImpl the EhcacheXAResourceImpl this Request will be used for
         * @param request the actual Request
         * @param xid
         */
        public XARequestCallable(EhcacheXAResourceImpl resourceImpl, XARequest request, Xid xid) {
            this.resourceImpl = resourceImpl;
            this.request = request;
            this.xid = xid;
        }

        /**
         *
         */
        public XAResponse call() throws Exception {
            Thread.currentThread().setName("XA-Request processor Thread Xid [ " + xid + " ]");

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
                    throw new EhcacheXAException("Unknown enum type: " + request.getRequestType(), XAException.XAER_RMERR);
            }
            } catch (XAException xaE) {
                xaException = xaE;
            } catch (Throwable t) {
                xaException = new EhcacheXAException("Some problem happened while processing xa request: " + request.getRequestType(),
                        XAException.XAER_RMERR, t);
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
