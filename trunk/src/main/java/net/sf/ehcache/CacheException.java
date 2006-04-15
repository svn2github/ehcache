/**
 *  Copyright 2003-2006 Greg Luck
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


package net.sf.ehcache;

/**
 * A runtime Cache Exception, compatible with JDK1.3
 * <p/>
 * Because JDK1.3 does not support chained exceptions or intial cause, this class has its own initialCause
 * field and {@link #getInitialCause} accessor, to aid with debugging. The JDK1.4 initial cause mechanism is
 * not used or populated. 
 * <p/>
 *
 * @author Greg Luck
 * @version $Id$
 * @revised 1.2
 */
public class CacheException extends RuntimeException {


    /**
     * Enables the cause to be recorded in a way that supports pre-JDK1.4 JDKs
     */
    private final Throwable initialCause;

    /**
     * Constructor for the CacheException object
     */
    public CacheException() {
        super();
        initialCause = null;
    }

    /**
     * Constructor for the CacheException object
     *
     * @param message
     */
    public CacheException(String message) {
        super(message);
        initialCause = null;
    }


    /**
     * Constructor for the CacheException object
     *
     * @param message
     */
    public CacheException(String message, Throwable initialCause) {
        super(message);
        this.initialCause = initialCause;
    }

    /**
     * The intiial cause of this Exception.
     * @return the cause or null if this exception has no deeper cause.
     */
    public Throwable getInitialCause() {
        return initialCause;
    }


}
