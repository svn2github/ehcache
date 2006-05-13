/**
 *  Copyright 2003-2006 Greg Luck
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

package net.sf.ehcache.constructs.web;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import net.sf.ehcache.constructs.web.filter.GzipFilter;

/**
 * A Serializable representation of a {@link HttpServletResponse}.
 *
 * @author <a href="mailto:amurdoch@thoughtworks.com">Adam Murdoch</a>
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class PageInfo implements Serializable {
    private static final Log LOG = LogFactory.getLog(PageInfo.class.getName());
    private static final int FOUR_KB = 4196;
    private static final int GZIP_MAGIC_NUMBER_BYTE_1 = 31;
    private static final int GZIP_MAGIC_NUMBER_BYTE_2 = -117;
    private final ArrayList headers = new ArrayList();
    private final ArrayList serializableCookies = new ArrayList();
    private String contentType;
    private byte[] gzippedBody;
    private byte[] ungzippedBody;
    private int statusCode;
    private boolean storeGzipped;

    /**
     * Creates a PageInfo.
     * <p/>
     *
     * @param statusCode
     * @param contentType
     * @param headers
     * @param cookies
     * @param body
     * @param storeGzipped set this to false for images and page fragments which should never
     *                     be gzipped.
     */
    public PageInfo(final int statusCode, final String contentType, final Collection headers, final Collection cookies,
                    final byte[] body, boolean storeGzipped) throws AlreadyGzippedException {
        this.headers.addAll(headers);
        this.headers.remove("Content-Encoding");
        final Collection cookieCollection = cookies;
        for (Iterator iterator = cookieCollection.iterator(); iterator.hasNext();) {
            final Cookie cookie = (Cookie) iterator.next();
            serializableCookies.add(new SerializableCookie(cookie));
        }

        this.contentType = contentType;
        this.storeGzipped = storeGzipped;
        try {
            if (storeGzipped) {
                //gunzip on demand
                ungzippedBody = null;
                if (isBodyParameterGzipped()) {
                    gzippedBody = body;
                } else {
                    gzippedBody = gzip(body);
                }
            } else {
                if (isBodyParameterGzipped()) {
                    throw new IllegalArgumentException("Non gzip content has been gzipped.");
                } else {
                    ungzippedBody = body;
                }
            }
        } catch (IOException e) {
            LOG.error("Error ungzipping gzipped body", e);
        }

        this.statusCode = statusCode;
    }

    /**
     * @param ungzipped the bytes to be gzipped
     * @return gzipped bytes
     */
    private byte[] gzip(byte[] ungzipped) throws IOException, AlreadyGzippedException {
        if (isGzipped(ungzipped)) {
            throw new AlreadyGzippedException("The byte[] is already gzipped. It should not be gzipped again.");
        }
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
        gzipOutputStream.write(ungzipped);
        gzipOutputStream.close();
        return bytes.toByteArray();
    }

    /**
     * The response body will be assumed to be gzipped if the GZIP header has been set.
     *
     * @return true if the body is gzipped
     */
    private boolean isBodyParameterGzipped() {
        for (int i = 0; i < headers.size(); i++) {
            String[] keyValuePair = (String[]) headers.get(i);
            if (keyValuePair[1].equals("gzip")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the first two bytes of the candidate byte array for the magic number 0x677a.
     * This magic number was obtained from /usr/share/file/magic. The line for gzip is:
     * <p/>
     * <code>
     * >>14    beshort 0x677a          (gzipped)
     * </code>
     *
     * @param candidate the byte array to check
     * @return true if gzipped, false if null, less than two bytes or not gzipped
     */
    public static boolean isGzipped(byte[] candidate) {
        if (candidate == null || candidate.length < 2) {
            return false;
        } else {
            return (candidate[0] == GZIP_MAGIC_NUMBER_BYTE_1 && candidate[1] == GZIP_MAGIC_NUMBER_BYTE_2);
        }
    }

    /**
     * @return the content type of the response.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * @return the gzipped version of the body if the content is storeGzipped, otherwise null
     */
    public byte[] getGzippedBody() {
        if (storeGzipped) {
            if (gzippedBody.length == GzipFilter.EMPTY_GZIPPED_CONTENT_SIZE)  {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Detected empty gzip body. Returning 0 bytes.");
                }
                return new byte[0];
            }
            return gzippedBody;
        } else {
            return null;
        }
    }

    /**
     * Returns the headers of the response.
     */
    public List getHeaders() {
        return headers;
    }

    /**
     * Returns the cookies of the response.
     */
    public List getSerializableCookies() {
        return serializableCookies;
    }

    /**
     * Returns the status code of the response.
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * @return the ungzipped version of the body. This gunzipped on demand when storedGzipped, otherwise
     *         the ungzipped body is returned.
     */
    public byte[] getUngzippedBody() throws IOException {
        if (storeGzipped) {
            return ungzip(gzippedBody);
        } else {
            return ungzippedBody;
        }
    }

    /**
     * A highly performant ungzip implementation. Do not refactor this without taking new timings.
     * See ElementTest in ehcache for timings
     *
     * @param gzipped the gzipped content
     * @return an ungzipped byte[]
     * @throws java.io.IOException
     */
    private byte[] ungzip(final byte[] gzipped) throws IOException {
        final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(gzipped.length);
        final byte[] buffer = new byte[FOUR_KB];
        int bytesRead = 0;
        while (bytesRead != -1) {
            bytesRead = inputStream.read(buffer, 0, FOUR_KB);
            if (bytesRead != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
        }
        byte[] ungzipped = byteArrayOutputStream.toByteArray();
        inputStream.close();
        byteArrayOutputStream.close();
        return ungzipped;
    }

    /**
     * @return true if there is a non null gzipped body
     */
    public boolean hasGzippedBody() {
        return (gzippedBody != null);
    }

    /**
     * @return true if there is a non null ungzipped body
     */
    public boolean hasUngzippedBody() {
        return (ungzippedBody != null);
    }

    /**
     * Returns true if the response is ok.
     */
    public boolean isOk() {
        return (statusCode == HttpServletResponse.SC_OK);
    }
}

