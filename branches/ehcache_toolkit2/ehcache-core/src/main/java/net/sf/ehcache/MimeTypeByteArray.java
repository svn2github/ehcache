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

import java.io.Serializable;

/**
 * A bean used to wrap byte[] values to be placed in an Element so as to preserve MIME type information.
 * <p/>
 * This class provides the means to bypass Java's serialization mechanism to as to store anything that can be turned
 * into bytes. It opens the way to non Java uses of ehcache.
 *
 * @author Greg Luck
 * @version $Id$
 */
public class MimeTypeByteArray implements Serializable {

    private String mimeType;
    private byte[] value;

    /**
     * Empty constructor, as required for JavaBeans
     */
    public MimeTypeByteArray() {
        //
    }

    /**
     * Full constructor
     *
     * @param mimeType any String that provides information as to the type of the value
     * @param value    an arbitrary binary value.
     */
    public MimeTypeByteArray(String mimeType, byte[] value) {
        this.mimeType = mimeType;
        this.value = value;
    }

    /**
     * @return a String that provides information as to the type of the value
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     *
     * @param mimeType any String that provides information as to the type of the value
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    /**
     * @return the value, which can be any arbitrary binary value.
     * @see #getMimeType()
     */
    public byte[] getValue() {
        return value;
    }

    /**
     * @param value an arbitrary binary value.
     */
    public void setValue(byte[] value) {
        this.value = value;
    }

}
