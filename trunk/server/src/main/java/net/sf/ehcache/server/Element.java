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

package net.sf.ehcache.server;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Greg Luck
 * @version $Id$
 */

@XmlRootElement
public class Element {
    private byte[] data;

    private String uri;

    private String mimeType;

    /**
     * Empty Constructor
     */
    public Element() {
    }

    /**
     * Full constructor
     * @param data
     * @param uri
     * @param mimeType
     */
    public Element(byte[] data, String uri, String mimeType) {
        setData(data);
        setUri(uri);
        setMimeType(mimeType);
    }

    /**
     * Sets the payload
     * @param data
     */
    private void setData(byte[] data) {
        this.data = data;
    }

    /**
     * Gets the payload
     * @return
     */
    public byte[] getData() {
        return data;
    }

    /**
     * Gets the URI for this resource
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     * Sets the URI for this resource
     * @param uri
     */
    public void setUri(String uri) {
        this.uri = uri;
    }

    /**
     * Gets the MIME Type.
     * @return
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * Sets the MIME Type
     * @param mimeType
     */
    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }
}
