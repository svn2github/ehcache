/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.CompoundOperation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Latency;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Statistic;
import org.terracotta.statistics.archive.Timestamped;

/**
 *
 * @author cdennis
 */
class NullCompoundOperation<T> implements CompoundOperation<T> {

    private static final CompoundOperation INSTANCE = new NullCompoundOperation();
    
    static <T> CompoundOperation<T> instance() {
        return INSTANCE;
    }
    
    private NullCompoundOperation() {
        //singleton
    }

    @Override
    public Operation component(T result) {
        return NullOperation.instance();
    }

    @Override
    public Operation compound(Set<T> results) {
        return NullOperation.instance();
    }
}

class NullOperation implements Operation {

    private static final Operation INSTANCE = new NullOperation();

    static final Operation instance() {
        return INSTANCE;
    }
    
    private NullOperation() {
        //singleton
    }
    
    @Override
    public long count() {
        return 0L;
    }

    @Override
    public Statistic<Double> rate() {
        return NullStatistic.<Double>instance();
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
        return NullStatistic.instance();
    }

    @Override
    public Statistic<Long> maximum() {
        return NullStatistic.instance();
    }

    @Override
    public Statistic<Double> average() {
        return NullStatistic.instance();
    }
}

class NullStatistic<T> implements Statistic {

    private static final Statistic INSTANCE = new NullStatistic();
    
    static <T> Statistic<T> instance() {
        return INSTANCE;
    }
    
    private NullStatistic() {
        //singleton
    }
    
    @Override
    public T value() {
        return null;
    }

    @Override
    public List<Timestamped<T>> history() throws UnsupportedOperationException {
        return Collections.emptyList();
    }
}