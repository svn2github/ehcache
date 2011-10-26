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

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

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

        private static String name = System.getProperty("appname");


        /**
         * Checks the listener is there
         */
        @Test
        public void testListenerExists() throws Exception {
            String url = "http://" + name + ".appspot.com/";
            System.out.println("URL: " + url);
			WebClient webClient = new WebClient();
			HtmlPage page = webClient.getPage(url);
            System.out.println("Response: " + page.asText());
			assertEquals(200, page.getWebResponse().getStatusCode());
            assertTrue(page.asText().indexOf("Welcome to") != 0);
			webClient.closeAllWindows();
        }
    }
