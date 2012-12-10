/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.CompoundOperation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import org.terracotta.statistics.OperationStatistic;

/**
 *
 * @author cdennis
 */
class CompoundOperationImpl<T extends Enum<T>> implements CompoundOperation<T> {
    
    private final OperationStatistic<T> source;
    
    private final Map<T, OperationImpl<T>> operations;    
    private final ConcurrentMap<Set<T>, OperationImpl<T>> compounds = new ConcurrentHashMap<Set<T>, OperationImpl<T>>();

    private final long averagePeriod;
    private final TimeUnit averageUnit;
    
    private final ScheduledExecutorService executor;
    private final int historySize;
    private final long historyPeriod;
    private final TimeUnit historyUnit;
    
    public CompoundOperationImpl(OperationStatistic<T> source, Class<T> type, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.source = source;
        
        this.averagePeriod = averagePeriod;
        this.averageUnit = averageUnit;
        this.executor = executor;
        this.historySize = historySize;
        this.historyPeriod = historyPeriod;
        this.historyUnit = historyUnit;
        
        this.operations = new EnumMap(type);
        for (T result : type.getEnumConstants()) {
            operations.put(result, new OperationImpl(source, EnumSet.of(result), averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit));
        }
    }

    public void start() {
        for (OperationImpl<?> o : operations.values()) {
            o.start();
        }
    }

    public void stop() {
        for (OperationImpl<?> o : operations.values()) {
            o.stop();
        }
    }

    @Override
    public Operation component(T result) {
        return operations.get(result);
    }

    @Override
    public Operation compound(Set<T> results) {
        Set<T> key = EnumSet.copyOf(results);
        OperationImpl<T> existing = compounds.get(key);
        if (existing == null) {
            OperationImpl<T> created = new OperationImpl(source, key, averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit);
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

    boolean expire(long expiryTime) {
        for (OperationImpl<?> o : operations.values()) {
            o.expire(expiryTime);
        }
        for (Iterator<OperationImpl<T>> it = compounds.values().iterator(); it.hasNext(); ) {
            if (it.next().expire(expiryTime)) {
                it.remove();
            }
        }
        return false;
    }
}
