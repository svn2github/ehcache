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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Test cases for the Element.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: ElementTest.java,v 1.1 2006/03/09 06:38:20 gregluck Exp $
 */
public class ElementTest extends AbstractCacheTest {
    private static final Log LOG = LogFactory.getLog(ElementTest.class.getName());



    /**
     * Checks serialization performance.
     * <p/>
     * {@link Element#getSerializedSize()} measures size by serializing, so this
     * can be used to measure JVM serialization speed.
     * <p/>
     * For 310232 bytes the average serialization time is 7 ms
     */
    public void testSerializationPerformanceByteArray() throws CacheException {
        Serializable key = new String("key");

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (int j = 0; j < 10000; j++) {
            try {
                bout.write("abcdefghijklmnopqrstv1234567890".getBytes());
            } catch (IOException e) {
                LOG.error("This should not happen");
            }
        }
        byte[] value = bout.toByteArray();

        Element element = new Element(key, value);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            element.getSerializedSize();
        }
        long elapsed = stopWatch.getElapsedTime() / 100;
        LOG.info("In-memory size in bytes: " + element.getSerializedSize()
                + " time to serialize in ms: " + elapsed);
        assertTrue("Large object clone takes more than than 100ms", elapsed < 100);
    }


    /**
     * Checks the serialization time for a large compound Java object
     * Serialization time was 126ms for a size of 349225
     */
    public void testSerializationPerformanceJavaObjects() throws Exception {
        //Set up object graphs
        HashMap map = new HashMap(10000);
        for (int j = 0; j < 10000; j++) {
            map.put("key" + j, new String[]{"adfdafs", "asdfdsafa", "sdfasdf"});
        }
        Element element = new Element("key1", map);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            element.getSerializedSize();
        }
        long elapsed = stopWatch.getElapsedTime() / 100;
        LOG.info("In-memory size in bytes: " + element.getSerializedSize()
                + " time to serialize in ms: " + elapsed);
        assertTrue("Large object clone took more than 500ms", elapsed < 500);
    }


    /**
     * Checks the expense of cloning a large object
     * Average clone time 175ms for a size of 349225
     */
    public void testCalculateClonePerformanceJavaObjects() throws Exception {
        //Set up object graphs
        HashMap map = new HashMap(10000);
        for (int j = 0; j < 10000; j++) {
            map.put("key" + j, new String[]{"adfdafs", "asdfdsafa", "sdfasdf"});
        }
        Element element = new Element("key1", map);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            element.clone();
        }
        long elapsed = stopWatch.getElapsedTime() / 100;
        LOG.info("Time to clone object in ms: " + elapsed);
        LOG.info("In-memory size in bytes: " + element.getSerializedSize()
                + " time to clone in ms: " + elapsed);
        assertTrue("Large object clone takes less than 1 second", elapsed < 1000);
    }

    /**
     * Checks serialization performance.
     * <p/>
     * {@link Element#getSerializedSize()} measures size by serializing, so this
     * can be used to measure JVM serialization speed.
     * <p/>
     * For 310232 bytes the average clone time is 50 ms, but on Mac JDK 1.5 i seems to have blown out to 116ms. It looks
     * like a performance regression.
     * <p/>
     */
    public void testClonePerformanceByteArray() throws CacheException, CloneNotSupportedException {
        Serializable key = new String("key");

        byte[] value = getTestByteArray();

        Element element = new Element(key, value);
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            element.clone();
        }
        long elapsed = stopWatch.getElapsedTime() / 100;
        LOG.info("In-memory size in bytes: " + element.getSerializedSize()
                + " time to serialize in ms: " + elapsed);
        assertTrue("Large object clone takes less than 130 milliseconds", elapsed < 180);
    }


    private byte[] getTestByteArray() {
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        for (int j = 0; j < 10000; j++) {
            try {
                bout.write("abcdefghijklmnopqrstv1234567890".getBytes());
            } catch (IOException e) {
                LOG.error("This should not happen");
            }
        }
        return bout.toByteArray();

    }

    /**
     * Tests the deserialization performance of an element containing a large byte[]
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testDeserializationPerformance() throws IOException, ClassNotFoundException {

        byte[] value = getTestByteArray();
        Element element = new Element("test", value);

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bout);
        oos.writeObject(element);
        byte[] serializedValue = bout.toByteArray();
        oos.close();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 100; i++) {
            ByteArrayInputStream bin = new ByteArrayInputStream(serializedValue);
            ObjectInputStream ois = new ObjectInputStream(bin);
            ois.readObject();
            ois.close();
        }
        long elapsed = stopWatch.getElapsedTime() / 100;
        LOG.info("In-memory size in bytes: " + serializedValue.length
                + " time to deserialize in ms: " + elapsed);
        assertTrue(elapsed < 30);
    }

}
