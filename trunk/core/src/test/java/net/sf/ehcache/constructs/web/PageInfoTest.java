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

import net.sf.ehcache.StopWatch;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * PageInfo is a {@link java.io.Serializable} representation of a {@link javax.servlet.http.HttpServletResponse}.
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public class PageInfoTest extends AbstractWebTest {
    private static final Logger LOG = Logger.getLogger(PageInfoTest.class.getName());

    private File testFile;

    /**
     * Create gzip file in tmp
     *
     * @throws Exception
     */
    @Before
    public void setUp() throws Exception {
        String testGzipFile = System.getProperty("java.io.tmpdir") + File.separator + "test.gzip";
        testFile = new File(testGzipFile);
        FileOutputStream fout = new FileOutputStream(testFile);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fout);
        for (int j = 0; j < 100; j++) {
            for (int i = 0; i < 1000; i++) {
                gzipOutputStream.write(i);
            }
        }
        gzipOutputStream.close();
    }

    /**
     * Remove the testFile
     *
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        testFile.delete();
    }

    /**
     * Tests what happens when we trick {@link PageInfo} in thinking the content is not gzipped
     * and it tries to gzip it again.
     * <p/>
     * This will happen in the wild if an upstream filter or servlet gzips the content but fails to
     * set the gzip header.
     * <p/>
     * The correct result is that the PageInfo constructor should throw a {@link AlreadyGzippedException}.
     *
     * @throws IOException
     */
    @Test
    public void testAttemptedDoubleGzip() throws IOException {
        byte[] gzip = getGzipFileAsBytes();
        try {
            new PageInfo(200, "text/plain", new ArrayList(), new ArrayList(), gzip, true);
            fail();
        } catch (AlreadyGzippedException e) {
            assertEquals("The byte[] is already gzipped. It should not be gzipped again.", e.getMessage());
        }

    }

    /**
     * Based on the gunzip1 implementation.
     * <p/>
     * Takes 9ms for the 100kb test document on the reference machine
     *
     * @throws IOException
     * @throws AlreadyGzippedException
     * @throws InterruptedException
     */
    @Test
    public void testUsedGunzipImplementationPerformance() throws IOException, AlreadyGzippedException, InterruptedException {
        byte[] gzip = getGzipFileAsBytes();
        Collection headers = new ArrayList();
        String[] header = new String[]{"Content-Encoding", "gzip"};
        headers.add(header);
        PageInfo pageInfo = new PageInfo(200, "text/plain", headers, new ArrayList(), gzip, true);
        long initialMemoryUsed = memoryUsed();
        StopWatch stopWatch = new StopWatch();
        int size = 0;
        long timeTaken = 0;
        long finalMemoryUsed = 0;
        long incrementalMemoryUsed = 0;
        byte[] ungzipped = null;

        //warmup JVM
        for (int i = 0; i < 5; i++) {
            ungzipped = pageInfo.getUngzippedBody();
            Thread.sleep(200);
        }
        stopWatch.getElapsedTime();

        for (int i = 0; i < 50; i++) {
            ungzipped = pageInfo.getUngzippedBody();
        }
        size = ungzipped.length;
        timeTaken = stopWatch.getElapsedTime() / 50;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);
        assertTrue(timeTaken < 30);
    }


    /**
     * Tests the performance of gunzip using a variety of implementations.
     */
    @Test
    public void testGunzipPerformance() throws IOException, InterruptedException {
        long initialMemoryUsed = memoryUsed();
        byte[] gzip = getGzipFileAsBytes();
        byte[] ungzipped = null;
        int size = 0;
        long timeTaken = 0;
        long finalMemoryUsed = 0;
        long incrementalMemoryUsed = 0;
        StopWatch stopWatch = new StopWatch();

        //warmup JVM
        for (int i = 0; i < 5; i++) {
            ungzipped = ungzip1(gzip);
            ungzipped = ungzip2(gzip);
            ungzipped = ungzip3(gzip);
            ungzipped = ungzip4(gzip);
            ungzipped = ungzip5(gzip);
            Thread.sleep(200);
        }

        stopWatch.getElapsedTime();
        for (int i = 0; i < 50; i++) {
            ungzipped = ungzip1(gzip);
        }
        size = ungzipped.length;
        timeTaken = stopWatch.getElapsedTime() / 50;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);

        stopWatch.getElapsedTime();
        ungzipped = ungzip2(gzip);
        for (int i = 0; i < 50; i++) {
            size = ungzipped.length;
        }
        timeTaken = stopWatch.getElapsedTime() / 50;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);

        stopWatch.getElapsedTime();
        ungzipped = ungzip3(gzip);
        for (int i = 0; i < 50; i++) {
            size = ungzipped.length;
        }
        timeTaken = stopWatch.getElapsedTime() / 50;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);

        stopWatch.getElapsedTime();
        for (int i = 0; i < 50; i++) {
            ungzipped = ungzip5(gzip);
        }
        size = ungzipped.length;
        timeTaken = stopWatch.getElapsedTime() / 50;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);

        //Throws out the numbers. Go last.
        stopWatch.getElapsedTime();
        for (int i = 0; i < 5; i++) {
            ungzipped = ungzip4(gzip);
        }
        size = ungzipped.length;
        timeTaken = stopWatch.getElapsedTime() / 5;
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gunzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
        assertEquals(100000, size);
    }

    /**
     * Tests the performance and correctness of gzip.
     */
    @Test
    public void testGzipPerformance() throws IOException, InterruptedException {
        long initialMemoryUsed = memoryUsed();
        byte[] gzip = getGzipFileAsBytes();
        byte[] ungzipped = null;
        int size = 0;
        long timeTaken = 0;
        long finalMemoryUsed = 0;
        long incrementalMemoryUsed = 0;
        StopWatch stopWatch = new StopWatch();

        ungzipped = ungzip1(gzip);
        stopWatch.getElapsedTime();
        for (int i = 0; i < 50; i++) {
            gzip = gzip(ungzipped);
        }
        timeTaken = stopWatch.getElapsedTime() / 50;
        ungzipped = ungzip1(gzip);
        size = ungzipped.length;
        assertEquals(100000, size);
        finalMemoryUsed = memoryUsed();
        incrementalMemoryUsed = finalMemoryUsed - initialMemoryUsed;
        LOG.info("Average gzip time: " + timeTaken
                + ". Memory used: " + incrementalMemoryUsed
                + ". Size: " + size);
    }


    /**
     * Tests that we can reliably determine if a byte[] is gzipped.
     * Note that this test demonstrates that byte[] can be gzipped multiple times
     * and are still gzipped i.e. though gzip has been run multiple times a valid
     * gzip file results, although ungzipping once won't be enough to get back to
     * uncompressed source byte[]
     */
    public void magicNumberTest() throws IOException {
        byte[] gzip = getGzipFileAsBytes();

        //A large generated gzip file
        assertTrue(PageInfo.isGzipped(gzip));
        //Short String
        assertTrue(PageInfo.isGzipped(gzip("The rain in spain".getBytes())));
        //Null
        assertTrue(!PageInfo.isGzipped(null));
        //Less than two bytes
        assertTrue(!PageInfo.isGzipped(new byte[]{0x11}));
        //Not Gzipped
        assertTrue(!PageInfo.isGzipped("This is not gzipped".getBytes()));
        //Double Gzipped
        assertTrue(PageInfo.isGzipped(gzip(getGzipFileAsBytes())));
        //Triple Gzipped
        assertTrue(PageInfo.isGzipped(gzip(gzip(getGzipFileAsBytes()))));

    }

    /**
     * @param ungzipped the bytes to be gzipped
     * @return gzipped bytes
     */
    private byte[] gzip(byte[] ungzipped) throws IOException {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(bytes);
        gzipOutputStream.write(ungzipped);
        gzipOutputStream.close();
        return bytes.toByteArray();
    }


    /**
     * A high performance implementation, although not as fast as gunzip3.
     * gunzips 100000 of ungzipped content in 9ms on the reference machine.
     * It does not use a fixed size buffer and is therefore suitable for arbitrary
     * length arrays.
     *
     * @param gzipped
     * @return
     * @throws IOException
     */
    public byte[] ungzip1(final byte[] gzipped) throws IOException {
        final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzipped));
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(gzipped.length);
        final byte[] buffer = new byte[4096];
        int bytesRead = 0;
        while (bytesRead != -1) {
            bytesRead = inputStream.read(buffer, 0, 4096);
            if (bytesRead != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
        }
        byte[] ungzipped = byteArrayOutputStream.toByteArray();
        inputStream.close();
        byteArrayOutputStream.close();
        return ungzipped;
    }

    private byte[] ungzip2(final byte[] gzip) throws IOException {
        final GZIPInputStream inputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        final byte[] buffer = new byte[1500000];
        int bytesRead = 0;
        int counter = 0;
        while (bytesRead != -1) {
            bytesRead = inputStream.read(buffer, counter, 4096);
            counter += bytesRead;
        }
        //Revert the last -1 when the end of stream was reached
        counter++;
        byte[] unzipped = new byte[counter];
        System.arraycopy(buffer, 0, unzipped, 0, counter);
        return unzipped;
    }

    private byte[] ungzip3(final byte[] gzip) throws IOException {
        int size = 0;
        int counter = 0;
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        int bytesRead = 0;
        byte[] buffer = new byte[500000];
        byte[] tempBuffer = new byte[4096];
        counter = 0;
        while (bytesRead != -1) {
            bytesRead = gzipInputStream.read(tempBuffer);
            if (bytesRead != -1) {
                System.arraycopy(tempBuffer, 0, buffer, counter, bytesRead);
                counter += bytesRead;
            }
        }
        gzipInputStream.close();
        size = counter;
        byte[] unzipped = new byte[size];
        System.arraycopy(buffer, 0, unzipped, 0, counter);
        return unzipped;
    }

    private byte[] ungzip4(final byte[] gzip) throws IOException {
        byte[] buffer = new byte[500000];
        int size = 0;

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(gzip);
        GZIPInputStream gzipInputStream = new GZIPInputStream(byteArrayInputStream);

        int nextByte = 0;
        int counter = 0;
        while (nextByte != -1) {
            nextByte = gzipInputStream.read();
            if (nextByte != -1) {
                buffer[counter] = (byte) nextByte;
                counter++;
                size = counter;
            }
        }

        gzipInputStream.close();
        byte[] unzipped = new byte[counter];
        System.arraycopy(buffer, 0, unzipped, 0, counter);
        return unzipped;
    }

    private byte[] ungzip5(final byte[] gzip) throws IOException {
        int size = 0;
        int counter = 0;
        GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(gzip));
        int bytesRead = 0;
        byte[] buffer = new byte[32768];
        byte[] tempBuffer = new byte[4096];
        counter = 0;
        while (bytesRead != -1) {
            bytesRead = gzipInputStream.read(tempBuffer);
            if (bytesRead != -1) {
                if (buffer.length < counter + bytesRead) {
                    byte[] newBuffer = new byte[buffer.length + 32768];
                    System.arraycopy(buffer, 0, newBuffer, 0, counter);
                    buffer = newBuffer;
                }
                System.arraycopy(tempBuffer, 0, buffer, counter, bytesRead);
                counter += bytesRead;
            }
        }
        gzipInputStream.close();
        size = counter;
        byte[] unzipped = new byte[size];
        System.arraycopy(buffer, 0, unzipped, 0, counter);
        return unzipped;
    }

    private long memoryUsed() {
        return Runtime.getRuntime().maxMemory() - Runtime.getRuntime().freeMemory();
    }

    private byte[] getGzipFileAsBytes() throws IOException {
        byte[] buffer = new byte[500000];
        FileInputStream fileInputStream = new FileInputStream(testFile);
        int fileSize = fileInputStream.read(buffer);
        byte[] gzip = new byte[fileSize];
        System.arraycopy(buffer, 0, gzip, 0, fileSize);
        return gzip;
    }

}
