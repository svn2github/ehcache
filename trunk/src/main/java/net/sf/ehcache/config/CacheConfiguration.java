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
package net.sf.ehcache.config;

import net.sf.ehcache.store.MemoryStoreEvictionPolicy;

import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent Cache configuration
 * e.g.
 * <cache name="testCache1"
 * maxElementsInMemory="10000"
 * eternal="false"
 * timeToIdleSeconds="3600"
 * timeToLiveSeconds="10"
 * overflowToDisk="true"
 * diskPersistent="true"
 * diskExpiryThreadIntervalSeconds="120"
 * />
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: CacheConfiguration.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class CacheConfiguration {

    /**
     * the name of the cache
     */
    protected String name;

    /**
     * the maximum objects to be held in the {@link net.sf.ehcache.store.MemoryStore}
     */
    protected int maxElementsInMemory;

    /**
     * The policy used to evict elements from the {@link net.sf.ehcache.store.MemoryStore}.
     * This can be one of:
     * <ol>
     * <li>LRU - least recently used
     * <li>LFU - Less frequently used
     * <li>FIFO - first in first out, the oldest element by creation time
     * </ol>
     * The default value is LRU
     *
     * @since 1.2
     */
    protected MemoryStoreEvictionPolicy memoryStoreEvictionPolicy;


    /**
     * Sets whether elements are eternal. If eternal,  timeouts are ignored and the element
     * is never expired.
     */
    protected boolean eternal;

    /**
     * the time to idle for an element before it expires. Is only used
     * if the element is not eternal.A value of 0 means do not check for idling.
     */
    protected int timeToIdleSeconds;

    /**
     * Sets the time to idle for an element before it expires. Is only used
     * if the element is not eternal. This attribute is optional in the configuration.
     * A value of 0 means do not check time to live.
     */
    protected int timeToLiveSeconds;

    /**
     * whether elements can overflow to disk when the in-memory cache
     * has reached the set limit.
     */
    protected boolean overflowToDisk;

    /**
     * For caches that overflow to disk, does the disk cache persist between CacheManager instances?
     */
    protected boolean diskPersistent;

    /**
     * The interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    protected long diskExpiryThreadIntervalSeconds;

    /**
     * The event listener factories added by BeanUtils
     */
    protected List cacheEventListenerConfigurations = new ArrayList();

    /**
     * Sets the name of the cache. This must be unique
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the maximum objects to be held in memory
     */
    public void setMaxElementsInMemory(int maxElementsInMemory) {
        this.maxElementsInMemory = maxElementsInMemory;
    }

    /**
     * Sets the eviction policy. An invalid argument will set it to null
     */
    public void setMemoryStoreEvictionPolicy(String memoryStoreEvictionPolicy) {
        this.memoryStoreEvictionPolicy = MemoryStoreEvictionPolicy.fromString(memoryStoreEvictionPolicy);
    }

    /**
     * Sets whether elements are eternal. If eternal,  timeouts are ignored and the element
     * is never expired.
     */
    public void setEternal(boolean eternal) {
        this.eternal = eternal;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used
     * if the element is not eternal.
     */
    public void setTimeToIdleSeconds(int timeToIdleSeconds) {
        this.timeToIdleSeconds = timeToIdleSeconds;
    }

    /**
     * Sets the time to idle for an element before it expires. Is only used
     * if the element is not eternal.
     */
    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    /**
     * Sets whether elements can overflow to disk when the in-memory cache
     * has reached the set limit.
     */
    public void setOverflowToDisk(boolean overflowToDisk) {
        this.overflowToDisk = overflowToDisk;
    }

    /**
     * Sets whether, for caches that overflow to disk,
     * the disk cache persist between CacheManager instances
     */
    public void setDiskPersistent(boolean diskPersistent) {
        this.diskPersistent = diskPersistent;
    }

    /**
     * Sets the interval in seconds between runs of the disk expiry thread.
     * <p/>
     * 2 minutes is the default.
     * This is not the same thing as time to live or time to idle. When the thread runs it checks
     * these things. So this value is how often we check for expiry.
     */
    public void setDiskExpiryThreadIntervalSeconds(int diskExpiryThreadIntervalSeconds) {
        this.diskExpiryThreadIntervalSeconds = diskExpiryThreadIntervalSeconds;
    }

    /**
     * Configuration for the CachePeerListenerFactoryConfiguration
     */
    public class CacheEventListenerFactoryConfiguration extends FactoryConfiguration {
    }

    /**
     * Used by BeanUtils to add cacheEventListenerFactory elements to the cache configuration.
     */
    public void addCacheEventListenerFactory(CacheEventListenerFactoryConfiguration factory) {
        cacheEventListenerConfigurations.add(factory);
    }

}
