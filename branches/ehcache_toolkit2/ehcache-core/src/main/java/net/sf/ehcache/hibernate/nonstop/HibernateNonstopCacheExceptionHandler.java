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

package net.sf.ehcache.hibernate.nonstop;

import net.sf.ehcache.constructs.nonstop.NonStopCacheException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class that takes care of {@link NonStopCacheException} that happens in hibernate module
 *
 * @author Abhishek Sanoujam
 *
 */
public final class HibernateNonstopCacheExceptionHandler {
    /**
     * Property name which set as "true" will throw exceptions on timeout with hibernate
     */
    public static final String HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY = "ehcache.hibernate.propagateNonStopCacheException";

    /**
     * Property name for logging the stack trace of the nonstop cache exception too. False by default
     */
    public static final String HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY = "ehcache.hibernate.logNonStopCacheExceptionStackTrace";

    private static final Logger LOGGER = LoggerFactory.getLogger(HibernateNonstopCacheExceptionHandler.class);

    private static final HibernateNonstopCacheExceptionHandler INSTANCE = new HibernateNonstopCacheExceptionHandler();

    /**
     * private constructor
     */
    private HibernateNonstopCacheExceptionHandler() {
        // private
    }

    /**
     * Returns the singleton instance
     *
     * @return the singleton instance
     */
    public static HibernateNonstopCacheExceptionHandler getInstance() {
        return INSTANCE;
    }

    /**
     * Handle {@link NonStopCacheException}.
     * If {@link HibernateNonstopCacheExceptionHandler#HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY} system property is set to true,
     * rethrows the {@link NonStopCacheException}, otherwise logs the exception. While logging, if
     * {@link HibernateNonstopCacheExceptionHandler#HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY} is set to true, logs the exception stack
     * trace too, otherwise logs the exception message only
     *
     * @param nonStopCacheException
     */
    public void handleNonstopCacheException(NonStopCacheException nonStopCacheException) {
        if (Boolean.getBoolean(HIBERNATE_THROW_EXCEPTION_ON_TIMEOUT_PROPERTY)) {
            throw nonStopCacheException;
        } else {
            if (Boolean.getBoolean(HIBERNATE_LOG_EXCEPTION_STACK_TRACE_PROPERTY)) {
                LOGGER.debug("Ignoring NonstopCacheException - " + nonStopCacheException.getMessage(), nonStopCacheException);
            } else {
                LOGGER.debug("Ignoring NonstopCacheException - " + nonStopCacheException.getMessage());
            }
        }
    }

}
