/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class GroupElementTest extends AbstractCacheTest {

    //private static final Logger LOG = Logger.getLogger(GroupElementTest.class.getName());

    /**
     * Tests that the constructor sets everything right.
     */
    @Test
    public void testConstructor() {

        GroupElement element = new GroupElement("key");
        assertEquals("key", element.getKey());

        //a value should be set up that is a set with zero elements
        assertNotNull(element.getObjectValue());
        assertTrue(element.getObjectValue() instanceof Set);
        assertEquals(0, ((Set) element.getValue()).size());

        //and there should be a special accessor for the Set
        assertNotNull(element.getMemberKeys());
        assertEquals(0, element.getMemberKeys().size());

        assertEquals(0L, element.getVersion());
        long now = System.currentTimeMillis();
        long MARGIN = 2000L;
        assertTrue(element.getCreationTime()-now<MARGIN);
        assertEquals(0L, element.getLastAccessTime());
        assertEquals(0L, element.getNextToLastAccessTime());
        assertEquals(0L, element.getLastUpdateTime());
        assertEquals(0L, element.getHitCount());

    }

    /**
     * Tests that the full constructor sets everything right.
     */
    @Test
    public void testFullConstructor() {

        Set<Object> groupMembership = new HashSet<Object>();
        groupMembership.add("m1");

        GroupElement element = new GroupElement("key", groupMembership, 1L, 123L, 1234L, 12345L, 123456L, 1234567L);
        assertEquals("key", element.getKey());
        assertEquals(groupMembership, element.getValue());
        assertEquals(1L, element.getVersion());
        assertEquals(123L, element.getCreationTime());
        assertEquals(1234L, element.getLastAccessTime());
        assertEquals(12345L, element.getNextToLastAccessTime());
        assertEquals(123456L, element.getLastUpdateTime());
        assertEquals(1234567L, element.getHitCount());

    }
}
