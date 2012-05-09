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
package net.sf.ehcache.writer;

import net.sf.ehcache.CacheException;

/**
 * An exception specifically for throwing exceptions that happen with a {@link CacheWriterManager} implementation.
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CacheWriterManagerException extends CacheException {
    /**
     * Construct a new exception
     * 
     * @param cause the underlying cause for this exception
     */
    public CacheWriterManagerException(RuntimeException cause) {
        super(cause);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public RuntimeException getCause() {
        return (RuntimeException)super.getCause();
    }
}
