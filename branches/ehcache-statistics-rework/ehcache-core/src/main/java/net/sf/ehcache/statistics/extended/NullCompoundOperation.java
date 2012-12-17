/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statistics.extended;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;

import org.terracotta.statistics.archive.Timestamped;


/**
 * The Class NullCompoundOperation.
 *
 * @param <T> the generic type
 * @author cdennis
 */
class NullCompoundOperation<T> implements Operation<T> {

    private static final Operation INSTANCE = new NullCompoundOperation();

    /**
     * Instance.
     *
     * @param <T> the generic type
     * @return the operation
     */
    static <T> Operation<T> instance() {
        return INSTANCE;
    }

    private NullCompoundOperation() {
        //singleton
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#component(java.lang.Object)
     */
    @Override
    public Result component(T result) {
        return NullOperation.instance();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#compound(java.util.Set)
     */
    @Override
    public Result compound(Set<T> results) {
        return NullOperation.instance();
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#ratioOf(java.util.Set, java.util.Set)
     */
    @Override
    public Statistic<Double> ratioOf(Set<T> numerator, Set<T> denomiator) {
        return NullStatistic.instance(Double.NaN);
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#setAlwaysOn(boolean)
     */
    @Override
    public void setAlwaysOn(boolean enable) {
        //no-op
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#setWindow(long, java.util.concurrent.TimeUnit)
     */
    @Override
    public void setWindow(long time, TimeUnit unit) {
        //no-op
    }

    /* (non-Javadoc)
     * @see net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation#setHistory(int, long, java.util.concurrent.TimeUnit)
     */
    @Override
    public void setHistory(int samples, long time, TimeUnit unit) {
        //no-op
    }
}

class NullOperation implements Result {

    private static final Result INSTANCE = new NullOperation();

    static final Result instance() {
        return INSTANCE;
    }

    private NullOperation() {
        //singleton
    }

    @Override
    public Statistic<Long> count() {
        return NullStatistic.instance(0L);
    }

    @Override
    public Statistic<Double> rate() {
        return NullStatistic.instance(Double.NaN);
    }

    @Override
    public Latency latency() throws UnsupportedOperationException {
        return NullLatency.instance();
    }
}


class NullLatency implements Latency {

    private static final Latency INSTANCE = new NullLatency();

    static Latency instance() {
        return INSTANCE;
    }

    private NullLatency() {
        //singleton
    }

    @Override
    public Statistic<Long> minimum() {
        return NullStatistic.instance(null);
    }

    @Override
    public Statistic<Long> maximum() {
        return NullStatistic.instance(null);
    }

    @Override
    public Statistic<Double> average() {
        return NullStatistic.instance(Double.NaN);
    }
}

class NullStatistic<T> implements Statistic {

    private static final Map<Object, Statistic<?>> common = new HashMap<Object, Statistic<?>>();
    static {
        common.put(Double.NaN, new NullStatistic<Double>(Double.NaN));
        common.put(Float.NaN, new NullStatistic<Float>(Float.NaN));
        common.put(Long.valueOf(0L), new NullStatistic<Long>(0L));
        common.put(null, new NullStatistic(null));
    }

    static <T> Statistic<T> instance(T value) {
        Statistic<T> cached = (Statistic<T>) common.get(value);
        if (cached == null) {
            return new NullStatistic<T>(value);
        } else {
            return cached;
        }
    }

    private final T value;

    private NullStatistic(T value) {
        this.value = value;
    }

    @Override
    public boolean active() {
        return false;
    }

    @Override
    public T value() {
        return value;
    }

    @Override
    public List<Timestamped<T>> history() throws UnsupportedOperationException {
        return Collections.emptyList();
    }
}