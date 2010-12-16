/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.store.compound;

import net.sf.ehcache.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A copy strategy that uses Serialization to copy the object graph
 * @author Alex Snaps
 */
public class SerializationCopyStrategy implements CopyStrategy {

    /**
     * @inheritDoc
     */
    public <T> T copy(final T value) {
        final T newValue;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos  = null;
        ObjectInputStream ois  = null;
        try {
                oos = new ObjectOutputStream(bout);
                oos.writeObject(value);
                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                ois = new ObjectInputStream(bin);
                newValue = (T)ois.readObject();
            } catch (Exception e) {
                throw new CacheException("When configured copyOnRead or copyOnWrite, a Store will only accept Serializable values", e);
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                    if (ois != null) {
                        ois.close();
                    }
                } catch (Exception e) {
                    // 
                }
            }
        return newValue;
    }
}
