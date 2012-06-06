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

/**
 * A lightweight non-thread-safe rate statistic implementation.
 *
 * @author Chris Dennis
 */
public class UnlockedRateStatistic extends AbstractRateStatistic {

  private volatile long count;
  private volatile long rateSampleTime = System.nanoTime();
  private volatile float rateSample = Float.NaN;

  private volatile long sampleRateMask;
  private volatile long previousSample;

  /**
   * Create an UnlockedRateStatistic instance with the given average period.
   *
   * @param averagePeriod average period
   * @param unit period time unit
   */
  public UnlockedRateStatistic(long averagePeriod, TimeUnit unit) {
    super(averagePeriod, unit);
  }

  /**
   * {@inheritDoc}
   */
  public void event() {
    long value = ++count;
    if ((value & sampleRateMask) == 0) {
      long now = System.nanoTime();
      long previous = rateSampleTime;
      if (now != previous && value > previousSample) {
        rateSampleTime = now;
        float nowRate = ((float) (value - previousSample) / (now - previous));
        previousSample = value;
        rateSample = iterateMovingAverage(nowRate, now, rateSample, previous);
        long suggestedSampleRateMask = Long.highestOneBit(Math.max(1L, (long) (getRateAveragePeriod() * rateSample))) - 1;
        if (suggestedSampleRateMask != sampleRateMask) {
          sampleRateMask = suggestedSampleRateMask;
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public long getCount() {
    return count;
  }

  /**
   * {@inheritDoc}
   */
  public float getRate() {
    long then = rateSampleTime;
    long lastSample = previousSample;
    float thenAverage = rateSample;
    long now = System.nanoTime();
    if (now == then) {
      return thenAverage;
    } else {
      float nowValue = ((float) (count - lastSample)) / (now - then);
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
}
