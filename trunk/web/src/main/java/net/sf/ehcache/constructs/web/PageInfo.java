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

package net.sf.ehcache.constructs.web;


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
import java.util.Date;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A Serializable representation of a {@link HttpServletResponse}.
 *
 * @author <a href="mailto:amurdoch@thoughtworks.com">Adam Murdoch</a>
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: PageInfo.java 744 2008-08-16 20:10:49Z gregluck $
 */
public class PageInfo implements Serializable {
    private static final Logger LOG = Logger.getLogger(PageInfo.class.getName());
    private static final int FOUR_KB = 4196;
    private static final int GZIP_MAGIC_NUMBER_BYTE_1 = 31;
    private static final int GZIP_MAGIC_NUMBER_BYTE_2 = -117;
    private final ArrayList responseHeaders = new ArrayList();
    private final ArrayList serializableCookies = new ArrayList();
    private String contentType;
    private byte[] gzippedBody;
    private byte[] ungzippedBody;
    private int statusCode;
    private boolean storeGzipped;
    private Date created;
    private long timeToLiveSeconds;
    private static final long ONE_YEAR_IN_SECONDS = 60 * 60 * 24 * 365;
    ;

    /**
     * Creates a PageInfo object representing the "page". 
     * <p/>
     *
     * @param statusCode
     * @param contentType
     * @param headers
     * @param cookies
     * @param body
     * @param storeGzipped      set this to false for images and page fragments which should never
     * @param timeToLiveSeconds the time to Live in seconds. 0 means maximum, which is one year per RFC2616.
     * @throws AlreadyGzippedException
     */
    public PageInfo(final int statusCode, final String contentType, final Collection headers, final Collection cookies,
                    final byte[] body, boolean storeGzipped, long timeToLiveSeconds) throws AlreadyGzippedException {
        if (headers != null) {
            this.responseHeaders.addAll(headers);
        }
        setTimeToLiveWithCheckForNeverExpires(timeToLiveSeconds);


        created = new Date();
        this.responseHeaders.remove("Content-Encoding");
        this.contentType = contentType;
        this.storeGzipped = storeGzipped;
        this.statusCode = statusCode;
        this.created = created;
        this.timeToLiveSeconds = timeToLiveSeconds;
        extractCookies(cookies);

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
            LOG.log(Level.SEVERE, "Error ungzipping gzipped body", e);
        }


    }

    /**
     * See http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html
     * To mark a response as "never expires," an origin server sends an Expires date approximately one year
     * from the time the response is sent. HTTP/1.1 servers SHOULD NOT send Expires dates more than one year
     * in the future.
     * @param timeToLiveSeconds accepts 0, which means eternal. If the time is 0 or > one year, it is set to one
     * year in accordance with the RFC.
     * <p/>
     * Note: PageInfo does not hold a reference to the ehcache Element and therefore does not know what the
     * Element ttl is. It would normally make most sense to set the TTL to the same as the element expiry.
     */
    protected void setTimeToLiveWithCheckForNeverExpires(long timeToLiveSeconds) {
        //0 means eternal
        if (timeToLiveSeconds == 0 || timeToLiveSeconds > ONE_YEAR_IN_SECONDS) {
            this.timeToLiveSeconds = ONE_YEAR_IN_SECONDS;
        } else {
            this.timeToLiveSeconds = timeToLiveSeconds;
        }
    }

    private void extractCookies(Collection cookies) {
        if (cookies != null) {
            for (Iterator iterator = cookies.iterator(); iterator.hasNext();) {
                final Cookie cookie = (Cookie) iterator.next();
                serializableCookies.add(new SerializableCookie(cookie));
            }
        }
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
        for (int i = 0; i < responseHeaders.size(); i++) {
            String[] keyValuePair = (String[]) responseHeaders.get(i);
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
            return gzippedBody;
        } else {
            return null;
        }
    }

    /**
     * Returns the headers of the response.
     */
    public List getResponseHeaders() {
        return responseHeaders;
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
     * Returns true if the response is Ok.
     *
     * @return true if the response code is 200.
     */
    public boolean isOk() {
        return (statusCode == HttpServletResponse.SC_OK);
    }


    /**
     * The <code>Date</code> this PageInfo object was created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * The time to live in seconds.
     *
     * @return the time to live, or 0 if the wrapping element is eternal
     */
    public long getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }
}

