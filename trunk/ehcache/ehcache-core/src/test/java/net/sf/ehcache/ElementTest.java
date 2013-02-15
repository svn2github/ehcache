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

package net.sf.ehcache;


import static junit.framework.Assert.fail;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test cases for the Element.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class ElementTest {

    private static final Logger LOG = LoggerFactory.getLogger(ElementTest.class.getName());


    /**
     * ehcache-1.2 adds support to Objects in addition to Serializable. Check that this works
     */
    @Test
    public void testObjectAccess() {
        Object key = new Object();
        Object value = new Object();
        Element element = new Element(key, value);
        //Should work
        assertEquals(key, element.getObjectKey());
        assertEquals(value, element.getObjectValue());

        //Should fail
        try {
            element.getKey();
        } catch (CacheException e) {
            //expected
        }
        assertEquals(value, element.getObjectValue());

    }

    /**
     * ehcache-1.1 and earlier exclusively uses Serializable keys and values. Check that this works
     */
    @Test
    public void testSerializableAccess() {
        Serializable key = "";
        Serializable value = "";
        Element element = new Element(key, value);

        //test gets
        assertEquals(key, element.getObjectKey());
        assertEquals(value, element.getObjectValue());

        //should also work with objects
        assertEquals(key, element.getObjectKey());
        assertEquals(value, element.getObjectValue());


    }

    /**
     * Tests that isSerializable does not blow up is if either key or value is null
     */
    @Test
    public void testIsSerializable() {

        Element element = new Element(null, null);
        assertTrue(element.isSerializable());


        Element elementWithNullValue = new Element("1", null);
        assertTrue(elementWithNullValue.isSerializable());


        Element elementWithNullKey = new Element(null, "1");
        assertTrue(elementWithNullValue.isSerializable());

        Element elementWithObjectKey = new Element(new Object(), "1");
        assertTrue(elementWithNullValue.isSerializable());


    }

    /**
     * Tests the robustness of equals
     */
    @Test
    public void testEquals() {

        Element element = new Element("key", "value");
        assertFalse(element.equals("dog"));
        assertTrue(element.equals(element));
        assertFalse(element.equals(null));
        assertFalse(element.equals(new Element("cat", "hat")));
    }

    /**
     * Tests that the full constructor sets everything right.
     */
    @Test
    public void testFullConstructor() {

        Element element = new Element("key", "value", 1L, 123L, 1234L, 12345L, 123456L);
        assertEquals("key", element.getKey());
        assertEquals("value", element.getValue());
        assertEquals(1L, element.getVersion());
        assertEquals(123L, element.getCreationTime());
        assertEquals(1234L, element.getLastAccessTime());
        assertEquals(12345L, element.getLastUpdateTime());
        assertEquals(123456L, element.getHitCount());

    }


    /**
     * Shows that null, regardless of class can be serialized.
     *
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testSerializable() throws IOException, ClassNotFoundException {

        String string1 = "string";
        String string2 = null;
        Object object1 = new Object();
        Object object2 = null;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(1000);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(baos);
        objectOutputStream.writeObject(string1);
        objectOutputStream.writeObject(string2);

        try {
            objectOutputStream.writeObject(object1);
            fail();
        } catch (NotSerializableException e) {
            //expected
        }
        try {
            objectOutputStream.writeObject(object2);
        } catch (NotSerializableException e) {
            //expected
        }


        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        ObjectInputStream objectInputStream = new ObjectInputStream(bais);
        assertEquals("string", objectInputStream.readObject());
        assertEquals(null, objectInputStream.readObject());
    }

    @Test
    public void testLastAccessTime() throws InterruptedException {
        Element element = new Element("", "");
        assertThat(element.getLastAccessTime(), is(0L));

        long time1 = System.currentTimeMillis();
        element.updateAccessStatistics();
        assertThat(element.getLastAccessTime() >= time1, is(true));

        TimeUnit.MILLISECONDS.sleep(100);

        long time2 = System.currentTimeMillis();
        element.resetAccessStatistics();
        assertThat(element.getLastAccessTime() >= time2, is(true));
    }

    @Test
    public void testCreateTime() throws InterruptedException {
        long time1 = System.currentTimeMillis();
        Element element = new Element("", "");
        assertThat(element.getCreationTime() >= time1, is(true));

        TimeUnit.MILLISECONDS.sleep(100);

        element.updateUpdateStatistics();
        assertThat(element.getLatestOfCreationAndUpdateTime() > element.getCreationTime(), is(true));

        TimeUnit.MILLISECONDS.sleep(100);
    }

    @Test
    public void testCloneForMetaData() throws CloneNotSupportedException {
        Element clone = (Element)new Element("", "", 1L, 12L, 123L, 1234L, 12345L).clone();
        assertThat(clone.getVersion(), is(1L));
        assertThat(clone.getCreationTime(), is(12L));
        assertThat(clone.getLastAccessTime(), is(123L));
        assertThat(clone.getLastUpdateTime(), is(0L));
        assertThat(clone.getHitCount(), is(12345L));
    }

}
