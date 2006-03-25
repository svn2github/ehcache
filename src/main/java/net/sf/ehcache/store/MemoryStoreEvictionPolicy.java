/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.store;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A typesafe enumeration of eviction policies
 * The policy used to evict elements from the {@link net.sf.ehcache.store.MemoryStore}.
 * This can be one of:
 * <ol>
 * <li>LRU - least recently used
 * <li>LFU - least frequently used
 * <li>FIFO - first in first out, the oldest element by creation time
 * </ol>
 * The default value is LRU
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: MemoryStoreEvictionPolicy.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 * @since 1.2
 */
public final class MemoryStoreEvictionPolicy {

    /**
     * LRU - least recently used
     */
    public static final MemoryStoreEvictionPolicy LRU = new MemoryStoreEvictionPolicy("LRU");

    /**
     * LFU - least frequently used
     */

    public static final MemoryStoreEvictionPolicy LFU = new MemoryStoreEvictionPolicy("LFU");

    /**
     * FIFO - first in first out, the oldest element by creation time
     */
    public static final MemoryStoreEvictionPolicy FIFO = new MemoryStoreEvictionPolicy("FIFO");

    private static final Log LOG = LogFactory.getLog(MemoryStoreEvictionPolicy.class.getName());

    // for debug only
    private final String myName;

    /**
     * This class should not be subclassed or have instances created
     * @param policy
     */
    private MemoryStoreEvictionPolicy(String policy) {
        myName = policy;
    }

    /**
     * @return a String representation of the policy
     */
    public String toString() {
        return myName;
    }

    /**
     * Converts a string representation of the policy into a policy.
     *
     * @param policy either LRU, LFU or FIFO
     * @return one of the static instances
     */
    public static MemoryStoreEvictionPolicy fromString(String policy) {
        if (policy != null) {
            if (policy.equalsIgnoreCase("LRU")) {
                return LRU;
            } else if (policy.equalsIgnoreCase("LFU")) {
                return LFU;
            } else if (policy.equalsIgnoreCase("FIFO")) {
                return FIFO;
            }
        }

        if (LOG.isWarnEnabled()) {
            LOG.warn("The memoryStoreEvictionPolicy of " + policy + " cannot be resolved. The policy will be" +
                    " set to LRU");
        }
        return LRU;
    }
}
