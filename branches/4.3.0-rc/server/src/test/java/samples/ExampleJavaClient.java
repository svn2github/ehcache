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

package samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A simple example Java client which uses the built-in java.net.URLConnection.
 *
 * @author BryantR
 * @author Greg Luck
 */
public final class ExampleJavaClient {

    private static final String EXAMPLE_CACHE_1 = "http://localhost:9090/ehcache/rest/tableColumn";
    private static final String EXAMPLE_ENTRY_1 = "http://localhost:9090/ehcache/rest/tableColumn/1";

    private static final Logger LOG = LoggerFactory.getLogger(ExampleJavaClient.class);

    private ExampleJavaClient() {
        //noop    
    }

    public static void main(String[] args) {
        try {
            createCache();
            int result = getCache();

            URL url;
            HttpURLConnection connection;
            InputStream is;
            createEntry(result);
            getEntry();


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void getEntry() throws IOException {
        URL url;
        HttpURLConnection connection;
        InputStream is;
        int result;
        url = new URL(EXAMPLE_ENTRY_1);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        is = connection.getInputStream();
        byte[] response2 = new byte[4096];
        result = is.read(response2);
        while (result != -1) {
            //System.out.write(response2, 0, result);
            result = is.read(response2);
        }
        if (is != null) {
            try {
                is.close();
            } catch (Exception e) {
                LOG.error("Exception on inputstream closeure", e);
            }
        }
        LOG.info("reading entry: " + connection.getResponseCode()
                + " " + connection.getResponseMessage());
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static void createEntry(int result) throws IOException {
        URL url;
        HttpURLConnection connection;
        url = new URL(EXAMPLE_ENTRY_1);
        connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "text/plain");
        connection.setDoOutput(true);
        connection.setRequestMethod("PUT");
        connection.connect();
        String sampleData = "ehcache is way cool!!!";
        byte[] sampleBytes = sampleData.getBytes();
        OutputStream os = null;
        os = connection.getOutputStream();
        os.write(sampleBytes, 0, sampleBytes.length);
        os.flush();
        LOG.info("result=" + result);
        LOG.info("creating entry: " + connection.getResponseCode()
                + " " + connection.getResponseMessage());
        if (connection != null) {
            connection.disconnect();
        }
    }

    private static int getCache() throws IOException {
        //get cache
        URL url = new URL(EXAMPLE_CACHE_1);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        InputStream is = connection.getInputStream();
        byte[] response1 = new byte[4096];
        int result = is.read(response1);
        while (result != -1) {
            //System.out.write(response1, 0, result);
            result = is.read(response1);
        }
        if (is != null) {
            try {
                is.close();
            } catch (Exception e) {
                LOG.error("Exception on inputstream closure", e);
            }
        }
        LOG.info("reading cache: " + connection.getResponseCode()
                + " " + connection.getResponseMessage());
        if (connection != null) {
            connection.disconnect();
        }
        return result;
    }

    private static void createCache() throws IOException {
        //create cache
        URL u = new URL(EXAMPLE_CACHE_1);
        HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
        urlConnection.setRequestMethod("PUT");

        int status = urlConnection.getResponseCode();
        LOG.info("Status: " + status);
        urlConnection.disconnect();
    }
}
