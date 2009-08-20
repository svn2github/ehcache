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

package net.sf.ehcache.distribution;


import net.sf.ehcache.AbstractCacheTest;
import net.sf.ehcache.Element;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests Serialization and SoftReferences in EventMessage
 *
 * @author Greg Luck
 * @version $Id$
 */
public class EventMessageTest {

    private static final Logger LOG = Logger.getLogger(EventMessageTest.class.getName());


    /**
     * SoftReference behaviour testing.
     */
    @Test
    public void testSoftReferences() {
        AbstractCacheTest.forceVMGrowth();
        Map map = new HashMap();
        for (int i = 0; i < 100; i++) {
            map.put(new Integer(i), new SoftReference(new byte[1000000]));
        }

        int counter = 0;
        for (int i = 0; i < 100; i++) {
            SoftReference softReference = (SoftReference) map.get(new Integer(i));
            byte[] payload = (byte[]) softReference.get();
            if (payload != null) {
                LOG.log(Level.INFO, "Value found for " + i);
                counter++;
            }
        }
        //This one varies by operating system and architecture. 
        assertTrue("You should get more than " + counter + " out of SoftReferences", counter >= 13);

    }

    /**
     * test serialization and deserialization of EventMessage.
     */
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {

        EventMessage eventMessage = new EventMessage(EventMessage.PUT, "key", new Element("key", "element"));

        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bout);
        oos.writeObject(eventMessage);
        byte[] serializedValue = bout.toByteArray();
        oos.close();
        EventMessage eventMessage2 = null;
        ByteArrayInputStream bin = new ByteArrayInputStream(serializedValue);
        ObjectInputStream ois = new ObjectInputStream(bin);
        eventMessage2 = (EventMessage) ois.readObject();
        ois.close();

        //Check after Serialization
        assertEquals("key", eventMessage2.getSerializableKey());
        assertEquals("element", eventMessage2.getElement().getObjectValue());
        assertEquals(EventMessage.PUT, eventMessage2.getEvent());
        assertTrue(eventMessage2.isValid());

    }


}
