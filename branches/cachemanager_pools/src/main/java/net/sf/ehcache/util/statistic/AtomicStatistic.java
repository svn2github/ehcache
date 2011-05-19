package net.sf.ehcache.util.statistic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class AtomicStatistic extends AbstractStatistic {
  
  private static final int RATE_SAMPLE_TIME_FLAG_BITS = 1;
  private static final long RATE_CALCULATION_FLAG = 0x1L;
  
  private final AtomicLong count = new AtomicLong(0L);
  private final AtomicLong rateSampleTime = new AtomicLong(System.nanoTime() << RATE_SAMPLE_TIME_FLAG_BITS);
  
  private volatile float rateSample = Float.NaN;

  private volatile long sampleRateMask = 0L;
  private volatile long previousSample = 0L;

  public AtomicStatistic(long averagePeriod, TimeUnit unit) {
    super(averagePeriod, unit);
  }
  
  public void event() {
    long value = count.incrementAndGet();
    if ((value & sampleRateMask) == 0L) {
      long now = System.nanoTime();
      long previous = startIncrementTime(now);
      try {
        float nowRate = ((float) (value - previousSample)) / (now - previous);
        rateSample = iterateMovingAverage(nowRate, now, rateSample, previous);
        previousSample = value;
        long suggestedSampleRateMask = Long.highestOneBit(Math.max(1L, (long) (rateAveragePeriod * rateSample))) - 1;
        if (suggestedSampleRateMask != sampleRateMask) {
          sampleRateMask = suggestedSampleRateMask;
        }
      } finally {
        finishIncrementTime(now);
      }
    }
  }

  public long getCount() {
    return count.get();
  }

  public float getRate() {
    long then;
    long lastSample;
    float thenAverage;
    do {
      then = startReadTime();
      lastSample = previousSample;
      thenAverage = rateSample;
    } while (!validateTimeRead(then));

    long now = System.nanoTime();
    float nowValue = ((float) (count.get() - lastSample)) / (now - then);

    return iterateMovingAverage(nowValue, now, thenAverage, then) * TimeUnit.SECONDS.toNanos(1);
  }
  
  private long startIncrementTime(long newTime) {
    while (true) {
      long current = rateSampleTime.get();
      if (((current & RATE_CALCULATION_FLAG) == 0) && rateSampleTime.compareAndSet(current, (newTime << RATE_SAMPLE_TIME_FLAG_BITS) | RATE_CALCULATION_FLAG)) {
        return current >>> RATE_SAMPLE_TIME_FLAG_BITS;
      }
    }
  }
  
  private void finishIncrementTime(long value) {
    if (!rateSampleTime.compareAndSet((value << RATE_SAMPLE_TIME_FLAG_BITS) | RATE_CALCULATION_FLAG, value << RATE_SAMPLE_TIME_FLAG_BITS)) {
      throw new AssertionError();
    }
  }
  
  private long startReadTime() {
    while (true) {
      long current = rateSampleTime.get();
      if ((current & RATE_CALCULATION_FLAG) == 0) {
        return current >>> RATE_SAMPLE_TIME_FLAG_BITS;
      }
    }
  }
  
  private boolean validateTimeRead(long current) {
    return rateSampleTime.get() == (current << RATE_SAMPLE_TIME_FLAG_BITS);
  }
}
