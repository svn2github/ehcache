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
public class ExampleJavaClient {

    private static String TABLE_COLUMN_BASE =
            "http://localhost:9090/ehcache/rest/tableColumn";
    private static String TABLE_COLUMN_ELEMENT =
            "http://localhost:9090/ehcache/rest/tableColumn/1";

    /**
     * Creates a new instance of EHCacheREST
     */
    public ExampleJavaClient() {
    }

    public static void main(String[] args) {
        URL url;
        HttpURLConnection connection = null;
        InputStream is = null;
        OutputStream os = null;
        int result = 0;
        try {
            //create cache
            URL u = new URL(TABLE_COLUMN_BASE);
            HttpURLConnection urlConnection = (HttpURLConnection) u.openConnection();
            urlConnection.setRequestMethod("PUT");

            int status = urlConnection.getResponseCode();
            System.out.println("Status: " + status);
            urlConnection.disconnect();

            //get cache
            url = new URL(TABLE_COLUMN_BASE);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            is = connection.getInputStream();
            byte[] response1 = new byte[4096];
            result = is.read(response1);
            while (result != -1) {
                System.out.write(response1, 0, result);
                result = is.read(response1);
            }
            if (is != null) try {
                is.close();
            } catch (Exception ignore) {
            }
            System.out.println("reading cache: " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
            if (connection != null) connection.disconnect();

            //create entry
            url = new URL(TABLE_COLUMN_ELEMENT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "text/plain");
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.connect();
            String sampleData = "ehcache is way cool!!!";
            byte[] sampleBytes = sampleData.getBytes();
            os = connection.getOutputStream();
            os.write(sampleBytes, 0, sampleBytes.length);
            os.flush();
            System.out.println("result=" + result);
            System.out.println("creating entry: " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
            if (connection != null) connection.disconnect();

            //get entry
            url = new URL(TABLE_COLUMN_ELEMENT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            is = connection.getInputStream();
            byte[] response2 = new byte[4096];
            result = is.read(response2);
            while (result != -1) {
                System.out.write(response2, 0, result);
                result = is.read(response2);
            }
            if (is != null) try {
                is.close();
            } catch (Exception ignore) {
            }
            System.out.println("reading entry: " + connection.getResponseCode()
                    + " " + connection.getResponseMessage());
            if (connection != null) connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (os != null) try {
                os.close();
            } catch (Exception ignore) {
            }
            if (is != null) try {
                is.close();
            } catch (Exception ignore) {
            }
            if (connection != null) connection.disconnect();
        }
    }
}
