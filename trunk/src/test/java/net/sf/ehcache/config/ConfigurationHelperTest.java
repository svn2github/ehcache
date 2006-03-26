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

import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.CacheManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;

/**
 * Tests programmatically constructed Configuration instances
 *
 * @author Greg Luck
 * @version $Id: ConfigurationHelperTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class ConfigurationHelperTest extends AbstractCacheTest {

    private static final Log LOG = LogFactory.getLog(ConfigurationHelperTest.class.getName());


    /**
     * Should not give exceptions
     */
    public void testValidParameters() {
        Configuration configuration = new Configuration();
        CacheConfiguration defaultCache = new CacheConfiguration();
        defaultCache.setEternal(false);

        ConfigurationHelper configurationHelper =
                new ConfigurationHelper(manager, configuration);
        assertNotNull(configurationHelper);
    }

    /**
     * Will fail if all params null
     */
    public void testNullParameters() {
        try {
            new ConfigurationHelper((CacheManager) null, null);
            fail();
        } catch (Exception e) {
            //expected
            LOG.debug("Expected exception " + e.getMessage() + ". Error was " + e.getMessage());
        }
    }

    /**
     * Test the expansion of Java system properties.
     * These can be mixed in with other path information, in which case they should be expanded and the other
     * path information catenatated.
     * @throws IOException
     */
    public void testDiskStorePathExpansion() throws IOException {
        DiskStoreConfiguration diskStore = new DiskStoreConfiguration();

        specificPathTest(diskStore, "java.io.tmpdir", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1", "java.io.tmpdir");
        specificPathTest(diskStore, "java.io.tmpdir/cacheManager1/", "java.io.tmpdir");
        specificPathTest(diskStore, "user.dir", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1", "user.dir");
        specificPathTest(diskStore, "user.dir/cacheManager1/", "user.dir");
        specificPathTest(diskStore, "user.home", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1", "user.home");
        specificPathTest(diskStore, "user.home/cacheManager1/", "user.home");


    }

    private void specificPathTest(DiskStoreConfiguration diskStore, String specifiedPath, String systemProperty) {
        diskStore.setPath(specifiedPath);
        String expandedPath = diskStore.getPath();
        assertTrue(expandedPath.indexOf(systemProperty) == -1);

        File diskDir = null;
        try {
            diskDir = new File(expandedPath);
            diskDir.mkdirs();
            assertTrue(diskDir.exists());
            assertTrue(diskDir.isDirectory());
        } finally {
            //delete only paths we created, not existing system paths, for repeatability
            if (diskDir.getPath().indexOf("cacheManager1") != -1) {
                diskDir.delete();
            }
        }
    }
}
