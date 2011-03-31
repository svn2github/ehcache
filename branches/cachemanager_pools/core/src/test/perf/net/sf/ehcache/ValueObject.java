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

package net.sf.ehcache;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Simple dumb Bean representing something that might be stored in ehcache.
 * 
 * @author jabley
 *
 */
public final class ValueObject implements Externalizable {
    
    private String mimeType;
    
    private byte[] b;
    
    /**
     * no-args constructor required for serialization.
     */
    public ValueObject() {
        
    }

    public ValueObject(String mimeType, byte[] b) {
        if (mimeType == null || b == null) {
            throw new IllegalArgumentException();
        }
        
        this.mimeType = mimeType;
        this.b = b;
    }

    /**
     * {@inheritDoc}
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        this.mimeType = (String) in.readObject();
        this.b = new byte[in.readInt()];
        in.readFully(b);
    }

    /**
     * {@inheritDoc}
     */
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(this.mimeType);
        out.writeInt(b.length);
        out.write(b);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ValueObject)) {
            return false;
        }
        
        /* For our purposes just now, comparing the distinct MIME type field is enough. */
        return this.mimeType.equals(((ValueObject) (obj)).mimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        
        /* c.f. the equals implementation and evolve in step as required. */
        return 37 + 17 * this.mimeType.hashCode();
    }
    
}
