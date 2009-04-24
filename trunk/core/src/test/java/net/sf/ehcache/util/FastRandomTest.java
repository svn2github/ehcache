package net.sf.ehcache.util;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Random;

/**
 * @author Greg Luck
 */
public class FastRandomTest {

    private static final Logger LOG = Logger.getLogger(FastRandomTest.class.getName());

    Random random = new Random();


    @Test
    public void testTwoPercent() throws InterruptedException {
        testRandom(.02f);
    }

    @Test
    public void testTenPercent() throws InterruptedException {
        testRandom(.1f);
    }

    @Test
    public void testFiftyPercent() throws InterruptedException {
        testRandom(.5f);
    }


    public void testRandom(float probability) throws InterruptedException {
        FastRandom fastRandom = new FastRandom(probability);
        AtomicInteger trues = new AtomicInteger();
        AtomicInteger falses = new AtomicInteger();
        for (int i = 0; i < 2000; i++) {
            long time = System.currentTimeMillis();
            boolean result = fastRandom.select(time);
            if (result) {
                trues.incrementAndGet();
            } else {
                falses.incrementAndGet();
            }

            Thread.sleep(random.nextInt(5));
        }
        LOG.info("Trues were: " + trues);
        LOG.info("Falses were: " + falses);
        float trueRatio = trues.floatValue() / 2000;
        LOG.info("True Ratio: " + trueRatio);

        assertTrue(trueRatio > (probability - 0.03) && trueRatio < (probability + 0.03));

    }


}
