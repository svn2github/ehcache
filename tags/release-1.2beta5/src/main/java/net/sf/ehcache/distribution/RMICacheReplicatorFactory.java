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
package net.sf.ehcache.distribution;

import net.sf.ehcache.event.CacheEventListener;
import net.sf.ehcache.event.CacheEventListenerFactory;
import net.sf.ehcache.util.PropertyUtil;

import java.util.Properties;

/**
 * Creates an RMICacheReplicator using properties. Config lines look like:
 * <pre>&lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
 * properties="
 * replicateAsynchronously=true,
 * replicatePuts=true
 * replicateUpdates=true
 * replicateUpdatesViaCopy=true
 * replicateRemovals=true
 * "/&gt;</pre>
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: RMICacheReplicatorFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class RMICacheReplicatorFactory extends CacheEventListenerFactory {
    private static final String REPLICATE_PUTS = "replicatePuts";
    private static final String REPLICATE_UPDATES = "replicateUpdates";
    private static final String REPLICATE_UPDATES_VIA_COPY = "replicateUpdatesViaCopy";
    private static final String REPLICATE_REMOVALS = "replicateRemovals";
    private static final String REPLICATE_ASYNCHRONOUSLY = "replicateAsynchronously";

    /**
     * Create a <code>CacheEventListener</code> which is also a CacheReplicator.
     * <p/>
     * The defaults if properties are not specified are:
     * <ul>
     * <li>replicatePuts=true
     * <li>replicateUpdates=true
     * <li>replicateUpdatesViaCopy=true
     * <li>replicateRemovals=true;
     * <li>replicateAsynchronously=true
     * </ul>
     *
     * @param properties implementation specific properties. These are configured as comma
     *                   separated name value pairs in ehcache.xml e.g.
     *                   <p/>
     *                   <code>
     *                   &lt;cacheEventListenerFactory class="net.sf.ehcache.distribution.RMICacheReplicatorFactory"
     *                   properties="
     *                   replicateAsynchronously=true,
     *                   replicatePuts=true
     *                   replicateUpdates=true
     *                   replicateUpdatesViaCopy=true
     *                   replicateRemovals=true
     *                   "/&gt;</code>
     * @return a constructed CacheEventListener
     */
    public CacheEventListener createCacheEventListener(Properties properties) {
        boolean replicatePuts = extractReplicatePuts(properties);
        boolean replicateUpdates = extractReplicateUpdates(properties);
        boolean replicateUpdatesViaCopy = extractReplicateUpdatesViaCopy(properties);
        boolean replicateRemovals = extractReplicateRemovals(properties);
        boolean replicateAsynchronously = extractReplicateAsynchronously(properties);

        if (replicateAsynchronously) {
            return new RMIAsynchronousCacheReplicator(
                    replicatePuts,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        } else {
            return new RMISynchronousCacheReplicator(
                    replicatePuts,
                    replicateUpdates,
                    replicateUpdatesViaCopy,
                    replicateRemovals);
        }
    }

    private boolean extractReplicateAsynchronously(Properties properties) {
        boolean replicateAsynchronously;
        String replicateAsynchronouslyString = PropertyUtil.extractAndLogProperty(REPLICATE_ASYNCHRONOUSLY, properties);
        if (replicateAsynchronouslyString != null) {
            replicateAsynchronously = parseBoolean(replicateAsynchronouslyString);
        } else {
            replicateAsynchronously = true;
        }
        return replicateAsynchronously;
    }

    private boolean extractReplicateRemovals(Properties properties) {
        boolean replicateRemovals;
        String replicateRemovalsString = PropertyUtil.extractAndLogProperty(REPLICATE_REMOVALS, properties);
        if (replicateRemovalsString != null) {
            replicateRemovals = parseBoolean(replicateRemovalsString);
        } else {
            replicateRemovals = true;
        }
        return replicateRemovals;
    }

    private boolean extractReplicateUpdatesViaCopy(Properties properties) {
        boolean replicateUpdatesViaCopy;
        String replicateUpdatesViaCopyString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES_VIA_COPY, properties);
        if (replicateUpdatesViaCopyString != null) {
            replicateUpdatesViaCopy = parseBoolean(replicateUpdatesViaCopyString);
        } else {
            replicateUpdatesViaCopy = true;
        }
        return replicateUpdatesViaCopy;
    }

    private boolean extractReplicateUpdates(Properties properties) {
        boolean replicateUpdates;
        String replicateUpdatesString = PropertyUtil.extractAndLogProperty(REPLICATE_UPDATES, properties);
        if (replicateUpdatesString != null) {
            replicateUpdates = parseBoolean(replicateUpdatesString);
        } else {
            replicateUpdates = true;
        }
        return replicateUpdates;
    }

    private boolean extractReplicatePuts(Properties properties) {
        boolean replicatePuts;
        String replicatePutsString = PropertyUtil.extractAndLogProperty(REPLICATE_PUTS, properties);
        if (replicatePutsString != null) {
            replicatePuts = parseBoolean(replicatePutsString);
        } else {
            replicatePuts = true;
        }
        return replicatePuts;
    }

    private static boolean parseBoolean(String name) {
        return ((name != null) && name.equalsIgnoreCase("true"));
    }
}
