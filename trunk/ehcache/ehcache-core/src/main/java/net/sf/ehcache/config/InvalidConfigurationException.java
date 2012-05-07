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

package net.sf.ehcache.config;

import net.sf.ehcache.CacheException;

import java.util.Collection;

/**
 * An exception to report invalid configuration settings.
 *  
 * @author gbevin
 * @author Greg Luck
 */
public class InvalidConfigurationException extends CacheException {

    /**
     * Constructs a new exception with a detailed message that explains the cause.
     * @param message the exception message
     */
    public InvalidConfigurationException(String message) {
        super(message);
    }

    /**
     * Constructs a new exception with a message containing all config errors
     * @param errors the list of error encountered
     */
    public InvalidConfigurationException(final Collection<ConfigError> errors) {
        this(null, errors);
    }

    /**
     * Constructs a new exception with a message containing all config errors
     * @param errors the list of error encountered
     */
    public InvalidConfigurationException(final String rootCause, final Collection<ConfigError> errors) {
        super(createErrorMessage(rootCause, errors));
    }

    private static String createErrorMessage(final String rootCause, Collection<ConfigError> errors) {
        final StringBuilder sb = new StringBuilder();
        if (rootCause == null) {
            sb.append("There ");
            if (errors.size() == 1) {
                sb.append("is one error ");
            } else {
                sb.append("are ")
                    .append(errors.size())
                    .append(" errors ");
            }
            sb.append("in your configuration: \n");
        } else {
            sb.append(rootCause).append('\n');
        }
        for (ConfigError error : errors) {
            sb.append("\t* ")
                .append(error.toString())
                .append('\n');
        }
        return sb
            .append("\n")
            .toString();
    }

}
