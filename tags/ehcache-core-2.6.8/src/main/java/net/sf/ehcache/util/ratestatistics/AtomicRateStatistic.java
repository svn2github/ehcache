/**
 *  Copyright Terracotta, Inc.
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

package net.sf.ehcache.util.ratestatistics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A thread-safe rate statistic implementation.
 *
 * @author Chris Dennis
 */
public class AtomicRateStatistic extends AbstractRateStatistic {

  private static final int SAMPLE_TIME_FLAG_BITS = 1;
  private static final long CALCULATION_FLAG = 0x1L;

  private final AtomicLong count = new AtomicLong(0L);
  private final AtomicLong rateSampleTime = new AtomicLong(getTime() << SAMPLE_TIME_FLAG_BITS);

  private volatile float rateSample = 0.0f;

  private volatile long sampleRateMask;
  private volatile long previousSample;

  /**
   * Create an AtomicRateStatistic instance with the given average period.
   *
   * @param averagePeriod average period
   * @param unit period time unit
   */
  public AtomicRateStatistic(long averagePeriod, TimeUnit unit) {
    super(averagePeriod, unit);
  }

  /**
   * {@inheritDoc}
   */
  public void event() {
    long value = count.incrementAndGet();
    if ((value & sampleRateMask) == 0L) {
      long now = getTime();
      long previous = startIncrementTime(now);
      try {
        if (now != previous && value > previousSample) {
          float nowRate = ((float)(value - previousSample)) / (now - previous);
          rateSample = iterateMovingAverage(nowRate, now, rateSample, previous);
          previousSample = value;
          long suggestedSampleRateMask = Long.highestOneBit(Math.max(1L, (long)(getRateAveragePeriod() * rateSample))) - 1;
          if (suggestedSampleRateMask != sampleRateMask) {
            sampleRateMask = suggestedSampleRateMask;
          }
        }
      } finally {
        finishIncrementTime(now);
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public long getCount() {
    return count.get();
  }

  /**
   * {@inheritDoc}
   */
  public float getRate() {
    long then;
    long lastSample;
    float thenAverage;
    do {
      then = startReadTime();
      lastSample = previousSample;
      thenAverage = rateSample;
    } while (!validateTimeRead(then));

    long now = getTime();
    if (now == then) {
      return thenAverage;
    } else {
      float nowValue = ((float) (count.get() - lastSample)) / (now - then);
      final float rate = iterateMovingAverage(nowValue, now, thenAverage, then) * TimeUnit.SECONDS.toNanos(1);
      if (Float.isNaN(rate)) {
        if (Float.isNaN(thenAverage)) {
          return 0f;
        } else {
          return thenAverage;
        }
      } else {
        return rate;
      }
    }
  }

  private long startIncrementTime(long newTime) {
    while (true) {
      long current = rateSampleTime.get();
      if (((current & CALCULATION_FLAG) == 0) && rateSampleTime.compareAndSet(current, (newTime << SAMPLE_TIME_FLAG_BITS) | CALCULATION_FLAG)) {
        return current >>> SAMPLE_TIME_FLAG_BITS;
      }
    }
  }

  private void finishIncrementTime(long value) {
    if (!rateSampleTime.compareAndSet((value << SAMPLE_TIME_FLAG_BITS) | CALCULATION_FLAG, value << SAMPLE_TIME_FLAG_BITS)) {
      throw new AssertionError();
    }
  }

  private long startReadTime() {
    while (true) {
      long current = rateSampleTime.get();
      if ((current & CALCULATION_FLAG) == 0) {
        return current >>> SAMPLE_TIME_FLAG_BITS;
      }
    }
  }

  private boolean validateTimeRead(long current) {
    return rateSampleTime.get() == (current << SAMPLE_TIME_FLAG_BITS);
  }


  private static long getTime() {
    return System.nanoTime();
  }
}
