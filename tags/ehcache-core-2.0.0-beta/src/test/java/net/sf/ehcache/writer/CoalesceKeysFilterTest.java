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

package net.sf.ehcache.writer;

import net.sf.ehcache.Element;
import net.sf.ehcache.writer.writebehind.CastingOperationConverter;
import net.sf.ehcache.writer.writebehind.CoalesceKeysFilter;
import net.sf.ehcache.writer.writebehind.operations.DeleteOperation;
import net.sf.ehcache.writer.writebehind.operations.KeyBasedOperation;
import net.sf.ehcache.writer.writebehind.operations.WriteOperation;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests for a key-based operations coalescing
 *
 * @author Geert Bevin
 * @version $Id$
 */
public class CoalesceKeysFilterTest {

    @Test
    public void testFilter() {
        List<KeyBasedOperation> operations = new ArrayList<KeyBasedOperation>();
        operations.add(new WriteOperation(new Element("key1", "value1"), 10));
        operations.add(new WriteOperation(new Element("key2", "value2"), 10));
        operations.add(new WriteOperation(new Element("key1", "value3"), 30));
        operations.add(new WriteOperation(new Element("key1", "value4"), 20));
        operations.add(new DeleteOperation("key3", 30));
        operations.add(new WriteOperation(new Element("key4", "value5"), 40));
        operations.add(new DeleteOperation("key2", 20));
        operations.add(new DeleteOperation("key4", 30));
        operations.add(new WriteOperation(new Element("key4", "value6"), 20));
        operations.add(new WriteOperation(new Element("key5", "value7"), 50));

        new CoalesceKeysFilter().filter(operations, CastingOperationConverter.getInstance());
        
        assertEquals(5, operations.size());
        assertEquals("key1", operations.get(0).getKey());
        assertEquals("value3", ((WriteOperation)operations.get(0)).getElement().getValue());

        assertTrue(operations.get(1) instanceof DeleteOperation);
        assertEquals("key3", operations.get(1).getKey());

        assertEquals("key4", operations.get(2).getKey());
        assertEquals("value5", ((WriteOperation)operations.get(2)).getElement().getValue());

        assertTrue(operations.get(3) instanceof DeleteOperation);
        assertEquals("key2", operations.get(3).getKey());

        assertEquals("key5", operations.get(4).getKey());
        assertEquals("value7", ((WriteOperation)operations.get(4)).getElement().getValue());
    }
}