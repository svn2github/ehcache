package com.myapp.test;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import static java.lang.Thread.sleep;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The idea of this test is to do a simple get against the deployed app on GAE.
 *
 * The demo app must be deployed first.
 *
 * While only a smoke test this is still useful, because the only environment that fully
 * enforces the many and varied GAE rules is GAE itself, not the local runtime.
 *
 * So this test will fail if a change is made to ehcache-core which breaks compatibility
 * with GAE.
 *
 * @author Greg Luck
 *
 */

public class GAEIntegrationTest {



        /**
         * Checks the listener is there
         */
        @Test
        public void testListenerExists() throws Exception {
            URL u = new URL("http://ehcache-g-a-e-demo.appspot.com/");
            HttpURLConnection httpURLConnection = (HttpURLConnection) u.openConnection();
            httpURLConnection.setRequestMethod("GET");

            assertEquals(200, httpURLConnection.getResponseCode());

            String responseBody = inputStreamToText(httpURLConnection.getInputStream());
            assertTrue(responseBody.indexOf("Welcome to") != 0);
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

    }
