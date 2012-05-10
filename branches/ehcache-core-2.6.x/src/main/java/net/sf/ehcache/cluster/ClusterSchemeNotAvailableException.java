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

package net.sf.ehcache.cluster;

import net.sf.ehcache.CacheException;

/**
 * Exception type that is thrown when requesting for a certain type of ClusterScheme and its not available.
 * 
 * @author Abhishek Sanoujam
 * 
 */
public class ClusterSchemeNotAvailableException extends CacheException {

    private final ClusterScheme unavailableClusterScheme;

    /**
     * Constructor accepting the {@link ClusterScheme} that is unavailable
     */
    public ClusterSchemeNotAvailableException(ClusterScheme unavailableClusterScheme) {
        super();
        this.unavailableClusterScheme = unavailableClusterScheme;
    }

    /**
     * Constructor accepting the {@link ClusterScheme} that is unavailable, message and root cause
     * 
     * @param message
     * @param cause
     */
    public ClusterSchemeNotAvailableException(ClusterScheme unavailableClusterScheme, String message, Throwable cause) {
        super(message, cause);
        this.unavailableClusterScheme = unavailableClusterScheme;
    }

    /**
     * Constructor accepting the {@link ClusterScheme} that is unavailable and message
     * 
     * @param message
     */
    public ClusterSchemeNotAvailableException(ClusterScheme unavailableClusterScheme, String message) {
        super(message);
        this.unavailableClusterScheme = unavailableClusterScheme;
    }

    /**
     * Constructor accepting the {@link ClusterScheme} that is unavailable and root cause
     * 
     * @param cause
     */
    public ClusterSchemeNotAvailableException(ClusterScheme unavailableClusterScheme, Throwable cause) {
        super(cause);
        this.unavailableClusterScheme = unavailableClusterScheme;
    }

    /**
     * Return the unavailable ClusterScheme this instance is associated with
     * 
     * @return Returns the unavailable ClusterScheme this instance is associated with
     */
    public ClusterScheme getUnavailableClusterScheme() {
        return unavailableClusterScheme;
    }

}
