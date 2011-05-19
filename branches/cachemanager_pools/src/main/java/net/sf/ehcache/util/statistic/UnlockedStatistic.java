package net.sf.ehcache.util.statistic;

import java.util.concurrent.TimeUnit;


public class UnlockedStatistic extends AbstractStatistic {
  
  private volatile long count = 0;
  private volatile long rateSampleTime = System.nanoTime();
  private volatile float rateSample = Float.NaN;

  private volatile long sampleRateMask = 0L;
  private volatile long previousSample = 0L;

  public UnlockedStatistic(long averagePeriod, TimeUnit unit) {
    super(averagePeriod, unit);
  }
  
  public void event() {
    long value = ++count;
    if ((value & sampleRateMask) == 0) {
      long now = System.nanoTime();
      long previous = rateSampleTime;
      rateSampleTime = now;
      float nowRate = ((float) (value - previousSample) / (now - previous));
      previousSample = value;
      rateSample = iterateMovingAverage(nowRate, now, rateSample, previous);
      long suggestedSampleRateMask = Long.highestOneBit(Math.max(1L, (long) (rateAveragePeriod * rateSample))) - 1;
      if (suggestedSampleRateMask != sampleRateMask) {
        sampleRateMask = suggestedSampleRateMask;
      }
    }
  }

  public long getCount() {
    return count;
  }
  
  public float getRate() {
    long then = rateSampleTime;
    long lastSample = previousSample;
    float thenAverage = rateSample;
    long now = System.nanoTime();
    float nowValue = ((float) (count - lastSample)) / (now - then);
    return iterateMovingAverage(nowValue, now, thenAverage, then) * TimeUnit.SECONDS.toNanos(1);
  }
}
