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
package net.sf.ehcache.event;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.AbstractCacheTest;

/**
 * Same as {@link CacheEventListenerTest} except that the listener is set programmatically. This test inherits because
 * all of the tests should behave identically.
 *
 * @author Greg Luck
 * @version $Id: ProgrammaticallyCreatedCacheEventListenerTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class ProgrammaticallyCreatedCacheEventListenerTest extends CacheEventListenerTest {
    private CountingCacheEventListener countingCacheEventListener = new CountingCacheEventListener();

    /**
     * {@inheritDoc}
     * @throws Exception
     */
    protected void setUp() throws Exception {
        CountingCacheEventListener.resetCounters();
        manager = CacheManager.create(AbstractCacheTest.TEST_CONFIG_DIR + "ehcache-nolisteners.xml");
        cache = manager.getCache(cacheName);
        cache.removeAll();
        //this call can be repeated. Attempts to further register the listener are ignored.
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
    }

    /**
     * An instance that <code>equals</code> one already registered is ignored
     */
    public void testAttemptDoubleRegistrationOfSameInstance() {
        cache.getCacheEventNotificationService().registerListener(countingCacheEventListener);
        //should just be the one from setUp
        assertEquals(1, cache.getCacheEventNotificationService().getCacheEventListeners().size());
    }

    /**
     * An new instance of the same class will be registered
     */
    public void testAttemptDoubleRegistrationOfSeparateInstance() {
        cache.getCacheEventNotificationService().registerListener(new CountingCacheEventListener());
        //should just be the one from setUp
        assertEquals(2, cache.getCacheEventNotificationService().getCacheEventListeners().size());
    }






}
