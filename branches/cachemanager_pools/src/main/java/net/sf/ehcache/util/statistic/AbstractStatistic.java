/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.util.statistic;

import java.util.concurrent.TimeUnit;

/**
 *
 * @author Chris Dennis
 */
abstract class AbstractStatistic implements Statistic {
  
  final long rateAveragePeriod;
  
  AbstractStatistic() {
    this(0, TimeUnit.SECONDS);
  }
  
  AbstractStatistic(long averagePeriod, TimeUnit unit) {
    this.rateAveragePeriod = unit.toNanos(averagePeriod);
  }
  
  float iterateMovingAverage(float nowValue, long now, float thenAverage, long then) {
    if (rateAveragePeriod == 0 || Float.isNaN(thenAverage)) {
      return nowValue;
    } else {
      float alpha = (float) alpha(now, then);
      return alpha * nowValue + (1 - alpha) * thenAverage;
    }
  }

  private double alpha(long now, long then) {
    return -Math.expm1(-((double) (now - then)) / rateAveragePeriod);
  }
}

