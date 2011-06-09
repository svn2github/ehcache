
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

package net.sf.ehcache.server.util;


import com.meterware.httpunit.ClientProperties;
import com.meterware.httpunit.HttpUnitOptions;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import org.apache.commons.httpclient.HttpMethod;
import org.dom4j.Document;
import org.dom4j.io.DOMReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 *
 * todo https://jira.terracotta.org/jira/browse/DEV-3939
 * A convenient base class for ehcache filter tests
 *
 * To run these you MUST be online due to a bug in glassfish embedded:
 *
 * 1) Do a maven package
 *
 * Running using Jetty
 * 1) comment out the BeforeClass and AfterClass methods in this class which uses Glassfish Embedded.
 * 2) mvn clean package
 * 3) mvn -Ptest -Denv=test jetty:run-war
 * 4) run your tests from the IDE
 *
 * Running using Glassfish
 * 1) run your tests from the IDE. Glassfish Embedded is included
 * @author Greg Luck
 * @version $Id: AbstractWebTest.java 796 2008-10-09 02:39:03Z gregluck $
 */
public final class WebTestUtil {

    /**
     * {@value}
     */
    public static final String CONTENT_TYPE = "CONTENT-TYPE";
    /**
     * {@value}
     */
    public static final String CONTENT_LENGTH = "CONTENT-LENGTH";
    /**
     * {@value}
     */
    public static final String CONTENT_ENCODING = "CONTENT-ENCODING";
    /**
     * {@value}
     */
    public static final String CONNECTION = "CONNECTION";
    /**
     * {@value}
     */
    public static final String SERVER = "SERVER";
    /**
     * {@value}
     */
    public static final String DATE = "DATE";
    /**
     * {@value}
     */
    public static final String KEEP_ALIVE = "KEEP-ALIVE";

    private static final Logger LOG = LoggerFactory.getLogger(WebTestUtil.class);

    private WebTestUtil() {
      //no-op
    }

    /**
     * Checks that the expected string occurs within the content string.
     */
    public static void assertContains(final String string, final String content) {
        if (content.indexOf(string) == -1) {
            throw new AssertionError(content + "' does not contain '" + string + "'");
        }
    }

    /**
     * Performs an HTTP request, and returns the response.
     * <p/>
     * The request is set to accept gzip encoding. Note that HttpUnit automatically gunzips the response
     * probived the "Content-encoding: gzip" response header is set.
     */
    public static WebResponse getResponseFromAcceptGzipRequest(final String uri) throws IOException, SAXException {
        final WebConversation conversation = createWebConversation(true);
        final WebResponse response = conversation.getResponse(buildUrl(uri));
        return response;
    }

    /**
     * Performs an HTTP request, and returns the response.
     */
    public static WebResponse getResponseFromNonAcceptGzipRequest(final String uri) throws IOException, SAXException {
        final WebConversation conversation = createWebConversation(false);
        final WebResponse response = conversation.getResponse(buildUrl(uri));
        return response;
    }

    /**
     * Creates a new WebConversation to use for this test.
     */
    public static WebConversation createWebConversation(boolean acceptGzip) {
        HttpUnitOptions.setExceptionsThrownOnScriptError(false);
        HttpUnitOptions.setCheckContentLength(true);
        HttpUnitOptions.setScriptingEnabled(false);
        ClientProperties.getDefaultProperties().setAcceptGzip(acceptGzip);
        final WebConversation conversation = new WebConversation();
        return conversation;
    }

    /**
     * Builds a URL from a URI
     *
     * @param uri
     * @return
     */
    private static String buildUrl(String uri) {
        return "http://localhost:9090" + uri;
    }

    /**
     * Checks:
     * <ol>
     * <li>The response code is OK i.e. 200
     * <li>The headers are sane.
     * <li>The page is not blank.
     * </ol>
     */
    public static void assertResponseGood(WebResponse response, boolean fullPage) {
        assertResponseOk(response);
        if (fullPage) {
            assertHeadersSane(response);
        } else {
            assertIncludeHeadersSane(response);
        }
        assertPageNotBlank(response);
        assertPropertlyFormed(response);
    }

    /**
     * Checks the response code is OK i.e. 200
     *
     * @param response
     */
    public static void assertResponseOk(WebResponse response) {
        assertEquals(HttpURLConnection.HTTP_OK, response.getResponseCode());
    }

    /**
     * Subtle problems can occur when headers disagree with the content.
     * <p/>
     * Check:
     * <ol>
     * <li>Content type is text/html
     * <li>The content length set in the header matches the actual content length
     * <li>If the header says the page is gzipped, it really is.
     * </ol>
     */
    public static void assertHeadersSane(WebResponse response) {
        //Content type is text/html with an optional character set
        String contentType = response.getHeaderField(CONTENT_TYPE);
        assertTrue(contentType.equals("text/html")
                || contentType.equals("text/html; charset=utf-8")
                || contentType.equals("text/html;charset=utf-8")
                || contentType.equals("text/html;charset=ISO-8859-1")
                || contentType.equals("text/html; charset=iso-8859-1"));

        //The content length set in the header matches the actual content length
        String headerContentLength = response.getHeaderField(CONTENT_LENGTH);
        if (headerContentLength != null) {
            assertEquals("Content length matches header content length",
                    Integer.parseInt(headerContentLength), response.getContentLength());
        }
    }

    /**
     * Subtle problems can occur when headers disagree with the content.
     *
     * @param response
     */
    public static void assertIncludeHeadersSane(WebResponse response) {
        String contentType = response.getHeaderField(CONTENT_TYPE);

        assertTrue(contentType == null
                || contentType.startsWith("text/html")
                || contentType.startsWith("text/plain"));

        String contentLength = response.getHeaderField(CONTENT_LENGTH);
        //assertTrue(contentLength == null || contentLength.equals("0"));
        assertNull(response.getHeaderField(CONTENT_ENCODING));
    }

    /**
     * Checks the included time produced comment on two pages to see if they
     * are the same. Being the same means that the pages were produced at exactly the same
     * time hence the page is cached.
     * <p/>
     * Relies on the existence of <code><!-- Generated at <%= date %> --></code> in the requested
     * page.
     */
    public static void checkTimeStamps(WebResponse webResponse1, WebResponse webResponse2,
                                   boolean shouldTimestampsBeEqual) throws Exception {
        LOG.debug("Should timestamps be equal: {}", shouldTimestampsBeEqual);
        String firstGeneratedTimestamp = getTimestamp(webResponse1);
        LOG.debug("First time stamp: {}", firstGeneratedTimestamp);
        String secondGeneratedTimestamp = getTimestamp(webResponse2);
        LOG.debug("Second time stamp: {}", secondGeneratedTimestamp);


        // Use assert equals because it provides more information if the assertion fails
        if (shouldTimestampsBeEqual) {
            assertEquals(firstGeneratedTimestamp, secondGeneratedTimestamp);
        } else {
            assertFalse(firstGeneratedTimestamp.equals(secondGeneratedTimestamp));
        }
    }

    /**
     * @return the server side rendering time stamp. Relies on the existence of a
     *         <br> <code>&lt;!-- Generated at <%= date %> --></code><br>
     *         scriptlet in the page.
     */
    public static String getTimestamp(final WebResponse response) throws Exception {
        String generationPrefix = "Generated at ";
        int index = response.getText().indexOf(generationPrefix);
        String timestamp = response.getText().substring(index, index + 36);
        return timestamp;
    }

    /**
     * Assert that the page cache was used.
     * The method for determining the page cache is being used is by looking at the first 'Generated at'
     * comment that appears.
     */
    public static void assertResponseGoodAndCached(String path, boolean fullPage) throws Exception {
        WebResponse firstResponse = getResponseFromAcceptGzipRequest(path);
        assertResponseGood(firstResponse, fullPage);
        sleep(TimeUnit.SECONDS, 2);
        WebResponse secondResponse = getResponseFromAcceptGzipRequest(path);
        assertResponseGood(secondResponse, fullPage);
        checkTimeStamps(firstResponse, secondResponse, true);
    }

    /**
     * Assert that the page cache was not used.
     * The method for determining the page cache is being used is by looking at the first 'Generated at'
     * comment that appears.
     */
    public static void assertResponseGoodAndNotCached(String path, boolean fullPage) throws Exception {
        WebResponse firstResponse = getResponseFromAcceptGzipRequest(path);
        assertResponseGood(firstResponse, fullPage);
        sleep(TimeUnit.SECONDS, 2);
        WebResponse secondResponse = getResponseFromAcceptGzipRequest(path);
        assertResponseGood(secondResponse, fullPage);
        checkTimeStamps(firstResponse, secondResponse, false);
    }

    /**
     * Checks the message on the form.
     */
    public static void checkMessage(final WebResponse response, final String expected)
            throws SAXException {
        final Document document = createDocument(response);
        final String message = document.valueOf("//*[@id='message']");
        assertEquals(expected, message);
    }

    /**
     * Creates a dom4j Document that represents the HTML body of a response.  This allows xpath expressions to be
     * used to extract details from the document.
     */
    public static Document createDocument(final WebResponse response) throws SAXException {
        response.getDOM();
        final Document doc = new DOMReader().read(response.getDOM());
        return doc;
    }

    /**
     * Runs a set of threads, for a fixed amount of time.
     */
    public static void runThreads(final List<Callable<?>> callable) throws Exception {
        final long endTime = System.currentTimeMillis() + 10000;
        final Throwable[] errors = new Throwable[1];

        // Spin up the threads
        final Thread[] threads = new Thread[callable.size()];
        for (int i = 0; i < threads.length; i++) {
            final Callable<?> executable = callable.get(i);
            threads[i] = new Thread() {
                public void run() {
                    try {
                        // Run the thread until the given end time
                        while (System.currentTimeMillis() < endTime) {
                            executable.call();
                        }
                    } catch (Throwable t) {
                        // Hang on to any errors
                        errors[0] = t;
                    }
                }
            };

            threads[i].start();
        }
        LOG.debug("Started {} threads.", threads.length);

        // Wait for the threads to finish
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }

        // Throw any error that happened
        if (errors[0] != null) {
            throw new Exception("Test thread failed.", errors[0]);
        }
    }

    /**
     * Checks for the page is not blank.
     * A blank page is a valid response but with a content of 0 bytes. It is a subtle error.
     * This can happen in certain cirumstances when no-one writes to the OutputStream at all.
     *
     * @param response
     */
    public static void assertPageNotBlank(WebResponse response) {
        String body = null;
        try {
            body = response.getText();
        } catch (IOException e) {
            LOG.error("", e);
            fail();
        }
        assertNotNull(body);
        assertTrue(!body.equals(""));
    }

    /**
     * Checks the response can be parsed into an XML document. Will fail if not properly formed.
     *
     * @param response
     */
    public static void assertPropertlyFormed(WebResponse response) {
//        try {
//            createDocument(response);
//        } catch (SAXException e) {
//            LOG.fatal(e.getMessage(), e);
//            fail();
//        }
        String text = null;
        try {
            text = response.getText();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            fail();
        }
        assertTrue(text.indexOf("<html>") != -1);
        assertTrue(text.indexOf("</html>") != -1);
    }

    /**
     * Orion returns a length of 0. Tomcat does not set content length at all.
     *
     * @param httpMethod
     */
    public static void checkNullOrZeroContentLength(HttpMethod httpMethod) {
        boolean nullContentLengthHeader = httpMethod.getResponseHeader("Content-Length") == null;
        if (!nullContentLengthHeader) {
            assertEquals("0", httpMethod.getResponseHeader("Content-Length").getValue());
        }
    }

    private static void sleep(TimeUnit unit, long length) {
      boolean interrupted = false;
      try {
        long duration = TimeUnit.MILLISECONDS.convert(length, unit);
        while (duration > 0) {
          long start = System.currentTimeMillis();
          try {
            Thread.sleep(duration);
          } catch (InterruptedException e) {
            interrupted = true;
          }
          long end = System.currentTimeMillis();
          duration -= (end - start);
        }
      } finally {
        if (interrupted) {
          Thread.currentThread().interrupt();
        }
      }
    }
}

