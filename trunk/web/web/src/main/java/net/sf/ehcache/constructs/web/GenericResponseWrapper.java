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

package net.sf.ehcache.constructs.web;

import net.sf.ehcache.constructs.web.filter.FilterServletOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Provides a wrapper for {@link javax.servlet.http.HttpServletResponseWrapper}.
 * <p/>
 * It is used to wrap the real Response so that we can modify it after
 * that the target of the request has delivered its response.
 * <p/>
 * It uses the Wrapper pattern.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: GenericResponseWrapper.java 793 2008-10-07 07:28:03Z gregluck $
 */
public class GenericResponseWrapper extends HttpServletResponseWrapper implements Serializable {

    private static final long serialVersionUID = -5976708169031065498L;

    private static final Logger LOG = LoggerFactory.getLogger(GenericResponseWrapper.class);
    private int statusCode = SC_OK;
    private int contentLength;
    private String contentType;
    private Map headerTracker = new HashMap();
    private final List headers = new ArrayList();
    private final List cookies = new ArrayList();
    private ServletOutputStream outstr;
    private PrintWriter writer;
    private boolean disableFlushBuffer;

    /**
     * Creates a GenericResponseWrapper
     */
    public GenericResponseWrapper(final HttpServletResponse response, final OutputStream outstr) {
        super(response);
        this.outstr = new FilterServletOutputStream(outstr);
    }

    /**
     * Gets the outputstream.
     */
    public ServletOutputStream getOutputStream() {
        return outstr;
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code) {
        statusCode = code;
        super.setStatus(code);
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i      the status code
     * @param string the error message
     * @throws IOException
     */
    public void sendError(int i, String string) throws IOException {
        statusCode = i;
        super.sendError(i, string);
    }

    /**
     * Send the error. If the response is not ok, most of the logic is bypassed and the error is sent raw
     * Also, the content is not cached.
     *
     * @param i the status code
     * @throws IOException
     */
    public void sendError(int i) throws IOException {
        statusCode = i;
        super.sendError(i);
    }

    /**
     * Send the redirect. If the response is not ok, most of the logic is bypassed and the error is sent raw.
     * Also, the content is not cached.
     *
     * @param string the URL to redirect to
     * @throws IOException
     */
    public void sendRedirect(String string) throws IOException {
        statusCode = HttpServletResponse.SC_MOVED_TEMPORARILY;
        super.sendRedirect(string);
    }

    /**
     * Sets the status code for this response.
     */
    public void setStatus(final int code, final String msg) {
        statusCode = code;
        LOG.warn("Discarding message because this method is deprecated.");
        super.setStatus(code);
    }

    /**
     * Returns the status code for this response.
     */
    public int getStatus() {
        return statusCode;
    }

    /**
     * Sets the content length.
     */
    public void setContentLength(final int length) {
        this.contentLength = length;
        super.setContentLength(length);
    }

    /**
     * Gets the content length.
     */
    public int getContentLength() {
        return contentLength;
    }

    /**
     * Sets the content type.
     */
    public void setContentType(final String type) {
        this.contentType = type;
        super.setContentType(type);
    }

    /**
     * Gets the content type.
     */
    public String getContentType() {
        return contentType;
    }


    /**
     * Gets the print writer.
     */
    public PrintWriter getWriter() throws IOException {
        if (writer == null) {
            writer = new PrintWriter(new OutputStreamWriter(outstr, getCharacterEncoding()), true);
        }
        return writer;
    }

    /**
     * Adds a header, even if one already exists, in accordance with the spec
     */
    public void addHeader(final String name, final String value) {
        final String[] header = new String[]{name, value};
        headers.add(header);

        Integer count = (Integer) headerTracker.get(name.toLowerCase());
        if (count == null) {
            count = new Integer(1);
        } else {
            count = new Integer(count.intValue() + 1);
        }
        headerTracker.put(name.toLowerCase(), count);
    }

    /**
     * Sets a header overwriting any previous values for the header if
     * it existed.
     */
    public void setHeader(final String name, final String value) {
        Integer count = (Integer) headerTracker.get(name);
        if (count != null && count.intValue() > 0) {
            for (int i = headers.size() - 1; i >= 0; i--) {
                String[] header = (String[]) headers.get(i);
                String hName = header[0];
                if (hName.equalsIgnoreCase(name)) {
                    if (count > 1) {
                        headers.remove(i);
                        count = count.intValue() - 1;
                        headerTracker.put(name.toLowerCase(), new Integer(count));
                    } else {
                        ((String[]) headers.get(i))[1] = value;
                    }

                }
            }
        } else {
            final String[] header = new String[]{name, value};
            headers.add(header);
            headerTracker.put(name.toLowerCase(), new Integer(1));
        }
    }

    /**
     * Gets the headers.
     */
    public Collection getHeaders() {
        return headers;
    }

    /**
     * Adds a cookie.
     */
    public void addCookie(final Cookie cookie) {
        cookies.add(cookie);
        super.addCookie(cookie);
    }

    /**
     * Gets all the cookies.
     */
    public Collection getCookies() {
        return cookies;
    }

    /**
     * Flushes buffer and commits response to client.
     */
    public void flushBuffer() throws IOException {
        flush();
        if (!disableFlushBuffer) {
            super.flushBuffer();
        }
    }

    /**
     * Resets the response.
     */
    public void reset() {
        super.reset();
        cookies.clear();
        headers.clear();
        statusCode = SC_OK;
        contentType = null;
        contentLength = 0;
    }

    /**
     * Resets the buffers.
     */
    public void resetBuffer() {
        super.resetBuffer();
    }

    /**
     * Flushes all the streams for this response.
     */
    public void flush() throws IOException {
        if (writer != null) {
            writer.flush();
        }
        outstr.flush();
    }

    /**
     * Is the wrapped reponse's buffer flushing disabled?
     * @return true if the wrapped reponse's buffer flushing disabled
     */
    public boolean isDisableFlushBuffer() {
        return disableFlushBuffer;
    }

    /**
     * Set if the wrapped reponse's buffer flushing should be disabled.
     * @param disableFlushBuffer true if the wrapped reponse's buffer flushing should be disabled
     */
    public void setDisableFlushBuffer(boolean disableFlushBuffer) {
        this.disableFlushBuffer = disableFlushBuffer;
    }
}

