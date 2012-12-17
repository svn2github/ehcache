/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statistics.extended;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;

import org.terracotta.statistics.OperationStatistic;

/**
 *
 * @author cdennis
 */
class CompoundOperationImpl<T extends Enum<T>> implements Operation<T> {
    
    private final OperationStatistic<T> source;
    
    private final Map<T, OperationImpl<T>> operations;    
    private final ConcurrentMap<Set<T>, OperationImpl<T>> compounds = new ConcurrentHashMap<Set<T>, OperationImpl<T>>();
    private final ConcurrentMap<List<Set<T>>, RatioStatistic> ratios = new ConcurrentHashMap<List<Set<T>>, RatioStatistic>();
    
    private final ScheduledExecutorService executor;

    private volatile long averageNanos;
    private volatile int historySize;    
    private volatile long historyNanos;
    
    private volatile boolean alwaysOn = false;
    
    public CompoundOperationImpl(OperationStatistic<T> source, Class<T> type, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = source;
        
        this.averageNanos = averageUnit.toNanos(averagePeriod);
        this.executor = executor;
        this.historySize = historySize;
        this.historyNanos = historyUnit.toNanos(historyPeriod);
        
        this.operations = new EnumMap(type);
        for (T result : type.getEnumConstants()) {
            operations.put(result, new OperationImpl(source, EnumSet.of(result), averageNanos, executor, historySize, historyNanos));
        }
    }

    @Override
    public Result component(T result) {
        return operations.get(result);
    }

    @Override
    public Result compound(Set<T> results) {
        if (results.size() == 1) {
            return component(results.iterator().next());
        } else {
            Set<T> key = EnumSet.copyOf(results);
            OperationImpl<T> existing = compounds.get(key);
            if (existing == null) {
                OperationImpl<T> created = new OperationImpl(source, key, averageNanos, executor, historySize, historyNanos);
                OperationImpl<T> racer = compounds.putIfAbsent(key, created);
                if (racer == null) {
                    return created;
                } else {
                    return racer;
                }
            } else {
                return existing;
            }
        }
    }

    @Override
    public Statistic<Double> ratioOf(Set<T> numerator, Set<T> denomiator) {
        List<Set<T>> key = Arrays.<Set<T>>asList(EnumSet.copyOf(numerator), EnumSet.copyOf(denomiator));
        RatioStatistic existing = ratios.get(key);
        if (existing == null) {
            RatioStatistic created = new RatioStatistic(compound(numerator).rate(), compound(denomiator).rate(), executor, historySize, historyNanos);
            RatioStatistic racer = ratios.putIfAbsent(key, created);
            if (racer == null) {
                return created;
            } else {
                return racer;
            }
        } else {
            return existing;
        }
    }

    @Override
    public void setAlwaysOn(boolean enable) {
        alwaysOn = enable;
        if (enable) {
            for (OperationImpl<T> op : operations.values()) {
                op.start();
            }
        }
    }

    @Override
    public void setWindow(long time, TimeUnit unit) {
        averageNanos = unit.toNanos(time);
        for (OperationImpl<T> op : operations.values()) {
            op.setWindow(averageNanos);
        }
        for (OperationImpl<T> op : compounds.values()) {
            op.setWindow(averageNanos);
        }
    }

    @Override
    public void setHistory(int samples, long time, TimeUnit unit) {
        historySize = samples;
        historyNanos = unit.toNanos(time);
        for (OperationImpl<T> op : operations.values()) {
            op.setHistory(historySize, historyNanos);
        }
        for (OperationImpl<T> op : compounds.values()) {
            op.setHistory(historySize, historyNanos);
        }
        for (RatioStatistic ratio : ratios.values()) {
            ratio.setHistory(historySize, historyNanos);
        }
    }

    void expire(long expiryTime) {
        if (!alwaysOn) {
            for (OperationImpl<?> o : operations.values()) {
                o.expire(expiryTime);
            }
            for (Iterator<OperationImpl<T>> it = compounds.values().iterator(); it.hasNext(); ) {
                if (it.next().expire(expiryTime)) {
                    it.remove();
                }
            }
            for (Iterator<RatioStatistic> it = ratios.values().iterator(); it.hasNext(); ) {
                if (it.next().expire(expiryTime)) {
                    it.remove();
                }
            }
        }
    }
}
