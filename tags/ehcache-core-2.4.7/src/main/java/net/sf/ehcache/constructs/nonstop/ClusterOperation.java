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

package net.sf.ehcache.constructs.nonstop;

import net.sf.ehcache.config.TimeoutBehaviorConfiguration.TimeoutBehaviorType;

/**
 * Interface for executing clustered operations (that can potentially get stuck)
 *
 * @author Abhishek Sanoujam
 *
 * @param <V>
 */
public interface ClusterOperation<V> {

    /**
     * Perform the actual operation.
     * This operation can potentially get stuck in the network. This method is called by an executor thread
     *
     * @return the return value depending on the implementation
     * @throws Exception
     */
    V performClusterOperation() throws Exception;

    /**
     * Perform action when the actual operation is not able to complete. Implementation should take care of what to do depending on the
     * {@link TimeoutBehaviorType}
     *
     * @param configuredTimeoutBehavior The configured {@link TimeoutBehaviorType}
     * @return value depending on the implementation
     */
    V performClusterOperationTimedOut(TimeoutBehaviorType configuredTimeoutBehavior);

}
