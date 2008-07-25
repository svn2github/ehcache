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

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.util.logging.Logger;

/**
 * Utilities used by tests
 * @author Greg Luck
 * @version $Id$
 */
public class HttpUtil {

    private static final Logger LOG = Logger.getLogger(HttpUtil.class.getName());

    private HttpUtil() {
        //utility class
    }

    public static HttpURLConnection get(String uri) throws IOException, ParserConfigurationException, SAXException {
        URL u = new URL(uri);
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("GET");

        int status = httpURLConnection.getResponseCode();
        LOG.info("Status " + status);
        String mediaType = httpURLConnection.getContentType();
        LOG.info("Content Type: " + mediaType);
        return httpURLConnection;
    }

    public static HttpURLConnection put(String uri) throws IOException {
        URL u = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("PUT");

        int status = urlConnection.getResponseCode();
        LOG.info("Status: " + status);
        urlConnection.disconnect();
        return urlConnection;
    }

    public static void put(String uri, String mediaType, InputStream in) throws IOException {
        URL u = new URL(uri);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        uc.setRequestMethod("PUT");
        uc.setRequestProperty("Content-Type", mediaType);
        uc.setDoOutput(true);

        OutputStream out = uc.getOutputStream();

        byte[] data = new byte[2048];
        int read;
        while ((read = in.read(data)) != -1)
            out.write(data, 0, read);
        out.close();

        int status = uc.getResponseCode();
        LOG.info("Status: " + status);
//        uc.disconnect();
    }

    public static void post(String uri, String mediaType, InputStream in) throws IOException {
        URL u = new URL(uri);
        HttpURLConnection uc = (HttpURLConnection) u.openConnection();
        uc.setRequestMethod("POST");
        uc.setRequestProperty("Content-Type", mediaType);
        uc.setDoOutput(true);

        OutputStream out = uc.getOutputStream();

        byte[] data = new byte[2048];
        int read;
        while ((read = in.read(data)) != -1)
            out.write(data, 0, read);
        out.close();

        int status = uc.getResponseCode();
        LOG.info("Status: " + status);
    }

    public static HttpURLConnection delete(String uri) throws IOException {
        URL u = new URL(uri);
        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("DELETE");

        int status = urlConnection.getResponseCode();
        LOG.info("Status: " + status);
        return urlConnection;
    }

    /**
     * Converts a response in an InputStream to a byte[] for easy manipulation
     */
    public static byte[] inputStreamToBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[10000];
        int r;
        while ((r = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, r);
        }
        return byteArrayOutputStream.toByteArray();
    }

    /**
     * Converts a response in an InputStream to a String for easy comparisons
     */
    public static String inputStreamToText(InputStream inputStream) throws IOException {
        byte[] bytes = inputStreamToBytes(inputStream);
        return new String(bytes);
    }


    public static HttpURLConnection head(String uri) throws IOException, ProtocolException {
        URL u = new URL(uri);
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("HEAD");

        int status = httpURLConnection.getResponseCode();
        LOG.info("Status " + status);
        String mediaType = httpURLConnection.getContentType();
        LOG.info("Content Type: " + mediaType);
        return httpURLConnection;
    }


    public static HttpURLConnection options(String uri) throws IOException, ProtocolException {
        URL u = new URL(uri);
        HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
        httpURLConnection.setRequestMethod("OPTIONS");

        int status = httpURLConnection.getResponseCode();
        LOG.info("Status " + status);
        String mediaType = httpURLConnection.getContentType();
        LOG.info("Content Type: " + mediaType);
        return httpURLConnection;
    }


}
