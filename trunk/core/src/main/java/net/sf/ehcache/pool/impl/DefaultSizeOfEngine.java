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

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSizeOfEngine.class.getName());
    private static final String USER_FILTER_RESOURCE = "net.sf.ehcache.sizeof.filter";

    private static final SizeOfFilter DEFAULT_FILTER;
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
            filters.add(getUserFilter());
        }
        DEFAULT_FILTER = new CombinationSizeOfFilter(filters.toArray(new SizeOfFilter[filters.size()]));
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
        this.maxDepth = maxDepth;
        this.abortWhenMaxDepthExceeded = abortWhenMaxDepthExceeded;
        SizeOf bestSizeOf;
        try {
            bestSizeOf = new AgentSizeOf(DEFAULT_FILTER);
            LOG.info("using Agent sizeof engine");
        } catch (UnsupportedOperationException e) {
            try {
                bestSizeOf = new UnsafeSizeOf(DEFAULT_FILTER);
                LOG.info("using Unsafe sizeof engine");
            } catch (UnsupportedOperationException f) {
                try {
                    bestSizeOf = new ReflectionSizeOf(DEFAULT_FILTER);
                    LOG.info("using Reflection sizeof engine");
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
            filterUrls.add(ClassLoaderUtil.getStandardClassLoader().getResource(USER_FILTER_RESOURCE));
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

    /**
     * {@inheritDoc}
     */
    public Size sizeOf(final Object key, final Object value, final Object container) {
        Size size = sizeOf.deepSizeOf(maxDepth, abortWhenMaxDepthExceeded, key, value, container);
        LOG.debug("size of {}/{}/{} -> {}", new Object[]{key, value, container, size.getCalculated()});
        return size;
    }
}
