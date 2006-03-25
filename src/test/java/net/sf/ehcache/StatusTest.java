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


package net.sf.ehcache;

import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Test cases for status.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: StatusTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class StatusTest extends TestCase {
    private static final Log LOG = LogFactory.getLog(StatusTest.class.getName());

    private static int int1 = 1;
    private int int2 = 2;
    private Status status1 = Status.STATUS_ALIVE;

    /**
     * The status is checked in almost every public method.
     * It has to be fast.
     * This test keeps it that way.
     */
    public void testEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();



        Status status2 = Status.STATUS_SHUTDOWN;

        for (int i = 0; i < 10000; i++) {
            status1.equals(status2);
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            status1.equals(status2);
        }
        long statusCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do equals(Status): " + statusCompareTime);
        assertTrue("Status compare is greater than permitted time", statusCompareTime < 15);

    }

    /**
     * An alternate implementation that is and override of the equals in Object. This would not normally
     * be used
     */
    public void testObjectEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();

        Object object = new Object();
        for (int i = 0; i < 10000; i++) {
            status1.equals(object);
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            status1.equals(object);
        }
        long objectCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do equals(Object): " + objectCompareTime);
        assertTrue("Status compare is greater than permitted time", objectCompareTime < 20);


    }


    /**
     * This was the implementation up to ehcache 1.2
     */
    public void testIntEqualsPerformance() {
        StopWatch stopWatch = new StopWatch();
        stopWatch.getElapsedTime();

        int2 = 12;
        boolean result;
        for (int i = 0; i < 10000; i++) {
            result = int1 == int2;
        }
        stopWatch.getElapsedTime();
        for (int i = 0; i < 10000; i++) {
            result = int1 == int2;
        }
        long intCompareTime = stopWatch.getElapsedTime();
        LOG.info("Time to do int == int: " + intCompareTime);
        assertTrue("Status compare is greater than permitted time", intCompareTime < 10);


    }




}
