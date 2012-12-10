/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.sf.ehcache.statisticsV2.extended;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.CompoundOperation;
import net.sf.ehcache.statisticsV2.extended.ExtendedStatistics.Operation;
import org.terracotta.statistics.SourceStatistic;
import org.terracotta.statistics.observer.OperationObserver;

/**
 *
 * @author cdennis
 */
class CompoundOperationImpl<T extends Enum<T>> implements CompoundOperation<T> {
    private final Map<T, OperationImpl<T>> operations;

    public CompoundOperationImpl(SourceStatistic<OperationObserver<T>> source, Class<T> type, long averagePeriod, TimeUnit averageUnit, ScheduledExecutorService executor, int historySize, long historyPeriod, TimeUnit historyUnit) {
        this.operations = new EnumMap(type);
        for (T result : type.getEnumConstants()) {
            operations.put(result, new OperationImpl(source, result, averagePeriod, averageUnit, executor, historySize, historyPeriod, historyUnit));
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
}
