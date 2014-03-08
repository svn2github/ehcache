package net.sf.ehcache.distribution;

import net.sf.ehcache.StopWatch;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;

import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Alex Snaps
 */
public class PayloadUtilPerfTest {

    private static final Logger LOG = LoggerFactory.getLogger(PayloadUtilPerfTest.class.getName());

    /**
     * 376 µs per one gzipping each time.
     * .1 µs if we compare hashCodes on the String and only gzip as necessary.
     *
     * @throws java.io.IOException
     * @throws InterruptedException
     */
    @Test
    public void testGzipSanityAndPerformance() throws IOException, InterruptedException {
        String payload = createReferenceString();
        // warmup vm
        for (int i = 0; i < 10; i++) {
            byte[] compressed = PayloadUtil.gzip(payload.getBytes());
            // make sure we don't forget to close the stream
            assertTrue(compressed.length > 300);
            Thread.sleep(20);
        }
        int hashCode = payload.hashCode();
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 10000; i++) {
            if (hashCode != payload.hashCode()) {
                PayloadUtil.gzip(payload.getBytes());
            }
        }
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("Gzip took " + elapsed / 10F + " µs");
    }

    /**
     * 169 µs per one.
     *
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void testUngzipPerformance() throws IOException, InterruptedException {
        String payload = createReferenceString();
        int length = payload.toCharArray().length;
        byte[] original = payload.getBytes();
        int byteLength = original.length;
        assertEquals(length, byteLength);
        byte[] compressed = PayloadUtil.gzip(original);
        // warmup vm
        for (int i = 0; i < 10; i++) {
            byte[] uncompressed = PayloadUtil.ungzip(compressed);
            uncompressed.hashCode();
            assertEquals(original.length, uncompressed.length);
            Thread.sleep(20);
        }
        StopWatch stopWatch = new StopWatch();
        for (int i = 0; i < 10000; i++) {
            PayloadUtil.ungzip(compressed);
        }
        long elapsed = stopWatch.getElapsedTime();
        LOG.info("Ungzip took " + elapsed / 10000F + " µs");
    }

    private String createReferenceString() {
        Configuration config = ConfigurationFactory.parseConfiguration(PayloadUtilPerfTest.class.getResource("/cachemanager-perf.xml"));
        Set<String> names = config.getCacheConfigurations().keySet();
        String urlBase = "//localhost.localdomain:12000/";
        StringBuilder buffer = new StringBuilder();
        for (String name : names) {
            buffer.append(urlBase);
            buffer.append(name);
            buffer.append("|");
        }
        String payload = buffer.toString();
        return payload;
    }

}
