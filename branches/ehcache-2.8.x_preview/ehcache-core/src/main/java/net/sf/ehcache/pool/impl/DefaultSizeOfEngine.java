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

package net.sf.ehcache.pool.impl;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.pool.Size;
import net.sf.ehcache.pool.SizeOfEngine;
import net.sf.ehcache.pool.sizeof.AgentSizeOf;
import net.sf.ehcache.pool.sizeof.ReflectionSizeOf;
import net.sf.ehcache.pool.sizeof.SizeOf;
import net.sf.ehcache.pool.sizeof.UnsafeSizeOf;
import net.sf.ehcache.pool.sizeof.MaxDepthExceededException;
import net.sf.ehcache.pool.sizeof.filter.AnnotationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.filter.CombinationSizeOfFilter;
import net.sf.ehcache.pool.sizeof.filter.ResourceSizeOfFilter;
import net.sf.ehcache.pool.sizeof.filter.SizeOfFilter;
import net.sf.ehcache.util.ClassLoaderUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Alex Snaps
 */
public class DefaultSizeOfEngine implements SizeOfEngine {

    /**
     * System property defining a user specific resource based size-of filter.
     * <p>
     * The resource pointed to by this property must be a list of fully qualified
     * field or class names, one per line:
     * <pre>
     * # This is a comment
     * org.mycompany.domain.MyType
     * org.mycompany.domain.MyOtherType.myField
     * </pre>
     * Fields or types matching against lines in this resource will be ignored
     * when calculating the size of the object graph.
     */
    public static final String USER_FILTER_RESOURCE = "net.sf.ehcache.sizeof.filter";
    
    private static final Logger LOG = LoggerFactory.getLogger(DefaultSizeOfEngine.class.getName());
    private static final String VERBOSE_DEBUG_LOGGING = "net.sf.ehcache.sizeof.verboseDebugLogging";

    private static final SizeOfFilter DEFAULT_FILTER;
    private static final boolean USE_VERBOSE_DEBUG_LOGGING;

    static {
        Collection<SizeOfFilter> filters = new ArrayList<SizeOfFilter>();
        filters.add(new AnnotationSizeOfFilter());
        try {
            filters.add(new ResourceSizeOfFilter(SizeOfEngine.class.getResource("builtin-sizeof.filter")));
        } catch (IOException e) {
            LOG.warn("Built-in sizeof filter could not be loaded: {}", e);
        }
        SizeOfFilter userFilter = getUserFilter();
        if (userFilter != null) {
            filters.add(userFilter);
        }
        DEFAULT_FILTER = new CombinationSizeOfFilter(filters.toArray(new SizeOfFilter[filters.size()]));

        USE_VERBOSE_DEBUG_LOGGING = getVerboseSizeOfDebugLogging();
    }

    private final SizeOf sizeOf;
    private final int maxDepth;
    private final boolean abortWhenMaxDepthExceeded;

    /**
     * Creates a default size of engine using the best available sizing algorithm.
     * @param maxDepth the max object graph that will be traversed.
     * @param abortWhenMaxDepthExceeded true if the object traversal should be aborted when the max depth is exceeded
     */
    public DefaultSizeOfEngine(int maxDepth, boolean abortWhenMaxDepthExceeded) {
        this(maxDepth, abortWhenMaxDepthExceeded, false);
    }

    /**
     * Creates a default size of engine using the best available sizing algorithm.
     * @param maxDepth the max object graph that will be traversed.
     * @param abortWhenMaxDepthExceeded true if the object traversal should be aborted when the max depth is exceeded
     * @param silent true if no info log explaining which agent was chosen should be printed
     */
    public DefaultSizeOfEngine(int maxDepth, boolean abortWhenMaxDepthExceeded, boolean silent) {
        this.maxDepth = maxDepth;
        this.abortWhenMaxDepthExceeded = abortWhenMaxDepthExceeded;
        SizeOf bestSizeOf;
        try {
            bestSizeOf = new AgentSizeOf(DEFAULT_FILTER);
            if (!silent) {
                LOG.info("using Agent sizeof engine");
            }
        } catch (UnsupportedOperationException e) {
            try {
                bestSizeOf = new UnsafeSizeOf(DEFAULT_FILTER);
                if (!silent) {
                    LOG.info("using Unsafe sizeof engine");
                }
            } catch (UnsupportedOperationException f) {
                try {
                    bestSizeOf = new ReflectionSizeOf(DEFAULT_FILTER);
                    if (!silent) {
                        LOG.info("using Reflection sizeof engine");
                    }
                } catch (UnsupportedOperationException g) {
                    throw new CacheException("A suitable SizeOf engine could not be loaded: " + e + ", " + f + ", " + g);
                }
            }
        }

        this.sizeOf = bestSizeOf;
    }

    private DefaultSizeOfEngine(DefaultSizeOfEngine defaultSizeOfEngine, int maxDepth, boolean abortWhenMaxDepthExceeded) {
        this.sizeOf = defaultSizeOfEngine.sizeOf;
        this.maxDepth = maxDepth;
        this.abortWhenMaxDepthExceeded = abortWhenMaxDepthExceeded;
    }

    /**
     * {@inheritDoc}
     */
    public SizeOfEngine copyWith(int maxDepth, boolean abortWhenMaxDepthExceeded) {
        return new DefaultSizeOfEngine(this, maxDepth, abortWhenMaxDepthExceeded);
    }

    private static SizeOfFilter getUserFilter() {
        String userFilterProperty = System.getProperty(USER_FILTER_RESOURCE);

        if (userFilterProperty != null) {
            List<URL> filterUrls = new ArrayList<URL>();
            try {
                filterUrls.add(new URL(userFilterProperty));
            } catch (MalformedURLException e) {
                LOG.debug("MalformedURLException using {} as a URL", userFilterProperty);
            }
            try {
                filterUrls.add(new File(userFilterProperty).toURI().toURL());
            } catch (MalformedURLException e) {
                LOG.debug("MalformedURLException using {} as a file URL", userFilterProperty);
            }
            filterUrls.add(ClassLoaderUtil.getStandardClassLoader().getResource(userFilterProperty));
            for (URL filterUrl : filterUrls) {
                SizeOfFilter filter;
                try {
                    filter = new ResourceSizeOfFilter(filterUrl);
                    LOG.info("Using user supplied filter @ {}", filterUrl);
                    return filter;
                } catch (IOException e) {
                    LOG.debug("IOException while loading user size-of filter resource", e);
                }
            }
        }
        return null;
    }

    private static boolean getVerboseSizeOfDebugLogging() {

        String verboseString = System.getProperty(VERBOSE_DEBUG_LOGGING, "false").toLowerCase();

        return verboseString.equals("true");
    }

    /**
     * {@inheritDoc}
     */
    public Size sizeOf(final Object key, final Object value, final Object container) {
        Size size;
        try {
            size = sizeOf.deepSizeOf(maxDepth, abortWhenMaxDepthExceeded, key, value, container);
        } catch (MaxDepthExceededException e) {
            LOG.warn(e.getMessage());
            LOG.warn("key type: " + key.getClass().getName());
            LOG.warn("key: " + key);
            LOG.warn("value type: " + value.getClass().getName());
            LOG.warn("value: " + value);
            LOG.warn("container: " + container);
            size = new Size(e.getMeasuredSize(), false);
        }

        if (USE_VERBOSE_DEBUG_LOGGING && LOG.isDebugEnabled()) {
            LOG.debug("size of {}/{}/{} -> {}", new Object[]{key, value, container, size.getCalculated()});
        }
        return size;
    }
}
