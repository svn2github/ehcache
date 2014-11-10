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

package net.sf.ehcache.writer;

import net.sf.ehcache.CacheEntry;
import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.CastingOperationConverter;
import net.sf.ehcache.writer.writebehind.CoalesceKeysFilter;
import net.sf.ehcache.writer.writebehind.operations.DeleteOperation;
import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;
import net.sf.ehcache.writer.writebehind.operations.WriteOperation;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests for a key-based operations coalescing
 *
 * @author Geert Bevin
 * @version $Id$
 */
@Category(CheckShorts.class)
public class CoalesceKeysFilterTest {

    @Test
    public void testFilter() {
        List<KeyBasedOperation> operations = new ArrayList<KeyBasedOperation>();
        operations.add(new WriteOperation(new Element("key1", "value1"), 10));
        operations.add(new WriteOperation(new Element("key2", "value2"), 10));
        operations.add(new WriteOperation(new Element("key1", "value3"), 30));
        operations.add(new WriteOperation(new Element("key1", "value4"), 20));
        operations.add(new DeleteOperation(new CacheEntry("key3", new Element("key3", "value5")), 30));
        operations.add(new WriteOperation(new Element("key4", "value6"), 40));
        operations.add(new DeleteOperation(new CacheEntry("key2", new Element("key2", "value7")), 20));
        operations.add(new DeleteOperation(new CacheEntry("key4", new Element("key4", "value8")), 30));
        operations.add(new WriteOperation(new Element("key4", "value9"), 20));
        operations.add(new WriteOperation(new Element("key5", "value10"), 50));
        
        // key1 - W 10, W 30, W 20 = 3
        // key2 - W 10, D 20 = 2
        // key3 - D 30 = 1
        // key4 - W 40, D 30, W 20 = 3
        // key5 - W 50 = 1
        // operations = k1, k2, k1, k2, k4, k1, k3, k4, k4, k5 = 10 (create time order)
        // operations = k1 10, k2 10, k1 30, k1 20, k3 30, k4 40, k2 20, k4 30, k4 20, k5 50 = 10 (add order)
        // operations = k1 30, k3 30, k4 40, k2 20, k5 50 = 5 

        new CoalesceKeysFilter().filter(operations, CastingOperationConverter.getInstance());

        assertEquals(5, operations.size());
        assertEquals("key1", operations.get(0).getKey());
        assertEquals("value3", ((WriteOperation) operations.get(0)).getElement().getValue());

        assertTrue(operations.get(1) instanceof DeleteOperation);
        assertEquals("key3", operations.get(1).getKey());

        assertEquals("key4", operations.get(2).getKey());
        assertEquals("value6", ((WriteOperation) operations.get(2)).getElement().getValue());

        assertTrue(operations.get(3) instanceof DeleteOperation);
        assertEquals("key2", operations.get(3).getKey());

        assertEquals("key5", operations.get(4).getKey());
        assertEquals("value10", ((WriteOperation) operations.get(4)).getElement().getValue());
    }
}