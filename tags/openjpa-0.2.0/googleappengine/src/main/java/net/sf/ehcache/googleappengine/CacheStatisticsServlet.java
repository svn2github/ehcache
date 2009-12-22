/**
 *  Copyright 2003-2009 Terracotta, Inc.
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


package net.sf.ehcache.googleappengine;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.memcache.Stats;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Statistics;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Display statistics about Ehcache and MemCache
 * todo: i18n, number formatting, etc...
 *
 * @author &eacute;drik LIME
 */
public class CacheStatisticsServlet extends HttpServlet {

    /**
     * Default constructor
     */
    public CacheStatisticsServlet() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getServletInfo() {
        return "GAE MemCache Statistics, Copyright (c) 2009 Terracotta";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain");
        setNoCache(response);
        PrintWriter out = response.getWriter();

        MemcacheService service = MemcacheServiceFactory.getMemcacheService();
        Stats mcStats = service.getStatistics();

        out.println("MemCache Statistics: " + service.getNamespace());

        out.print("ItemCount: ");
        out.print(mcStats.getItemCount());
        out.print(" (");
        out.print(mcStats.getTotalItemBytes());
        out.println(" bytes)");

        out.print("HitCount:  ");
        out.print(mcStats.getHitCount());
        out.print(" (");
        out.print(mcStats.getBytesReturnedForHits());
        out.println(" bytes)");

        out.print("MissCount: ");
        out.println(mcStats.getMissCount());

        out.print("MaxTimeWithoutAccess: ");
        out.print(mcStats.getMaxTimeWithoutAccess());
        out.println(" ms");


        out.println();


        for (CacheManager ehCacheManager : CacheManager.ALL_CACHE_MANAGERS) {
            String ehCacheManagerName = ehCacheManager.getName() + " (" + ehCacheManager.getStatus().toString() + ')';
            String[] ehCacheNames = ehCacheManager.getCacheNames();
            for (String ehCacheName : ehCacheNames) {
                out.println("Ehcache statistics " + ehCacheManagerName + ": " + ehCacheName);
                Ehcache ehCache = ehCacheManager.getEhcache(ehCacheName);
                Statistics ehStats = ehCache.getStatistics();

                //out.print("Guid: ");
                //out.println(ehCache.getGuid());
                out.print("StatisticsAccuracy: ");
                out.println(ehStats.getStatisticsAccuracyDescription());
                out.print("ObjectCount: ");
                out.print(ehStats.getObjectCount());
                out.print(" (memory: ");
                out.print(ehStats.getMemoryStoreObjectCount());
                out.print('/');
                out.print(ehCache.getCacheConfiguration().getMaxElementsInMemory());
                out.print(" -- disk: ");
                out.print(ehStats.getDiskStoreObjectCount());
                out.print('/');
                out.print(ehCache.getCacheConfiguration().getMaxElementsOnDisk());
                out.println(')');
                out.print("CacheHits:   ");
                out.print(ehStats.getCacheHits());
                out.print(" (memory: ");
                out.print(ehStats.getInMemoryHits());
                out.print(" -- disk: ");
                out.print(ehStats.getOnDiskHits());
                out.println(')');
                out.print("CacheMisses: ");
                out.println(ehStats.getCacheMisses());
                out.print("AverageGetTime: ");
                out.print(ehStats.getAverageGetTime());
                out.println(" ms");
                out.print("EvictionCount: ");
                out.println(ehStats.getEvictionCount());

                out.println();
            }
        }

        out.flush();
        out.close();
    }

    /**
     * @param response
     */
    public void setNoCache(HttpServletResponse response) {
        // <strong>NOTE</strong> - This header will be overridden
        // automatically if a <code>RequestDispatcher.forward()</code> call is
        // ultimately invoked.
        //resp.setHeader("Pragma", "No-cache"); // HTTP 1.0 //$NON-NLS-1$ //$NON-NLS-2$
        // HTTP 1.1 //$NON-NLS-1$
        response.setHeader("Cache-Control", "no-cache,no-store,max-age=0");
        // should we decide to enable caching, here are the current vary:
        response.addHeader("Vary", "Accept-Language,Accept-Encoding,Accept-Charset");
    }
}
