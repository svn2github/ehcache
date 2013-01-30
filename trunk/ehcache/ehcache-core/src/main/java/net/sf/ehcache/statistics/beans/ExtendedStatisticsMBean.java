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

package net.sf.ehcache.statistics.beans;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import net.sf.ehcache.Ehcache;
import net.sf.ehcache.statistics.extended.ExtendedStatistics;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Operation;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Result;
import net.sf.ehcache.statistics.extended.ExtendedStatistics.Statistic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.statistics.ValueStatistic;
import org.terracotta.statistics.archive.Timestamped;

/**
 * A dynamically built mbean based on the available statistics for a cache.
 * 
 * @author cschanck
 * 
 */
public class ExtendedStatisticsMBean extends ProxiedDynamicMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtendedStatisticsMBean.class);

    /**
     * Instantiates a new extended statistics m bean.
     * 
     * @param cache the cache
     * @param extendedStatistics the extended statistics
     */
    public ExtendedStatisticsMBean(Ehcache cache, ExtendedStatistics extendedStatistics) {
        super(divineName(cache), "Extended statistics for " + divineName(cache), Collections.EMPTY_LIST);
        LinkedList<AttributeProxy> proxies = new LinkedList<AttributeProxy>();
        processMethods(extendedStatistics, proxies);
        initialize(proxies);
    }

    private void processMethods(ExtendedStatistics extendedStatistics, List<AttributeProxy> proxies) {
        for (Method m : ExtendedStatistics.class.getDeclaredMethods()) {
            try {
                extractAttributes(extendedStatistics, proxies, m);
            } catch (IllegalArgumentException e) {
                LOGGER.info(e.getMessage());
            } catch (IllegalAccessException e) {
                LOGGER.info(e.getMessage());
            } catch (InvocationTargetException e) {
                LOGGER.info(e.getMessage());
            }
        }
    }

    private void extractAttributes(ExtendedStatistics extendedStatistics, List<AttributeProxy> proxies, Method m)
            throws IllegalAccessException, InvocationTargetException {
        if (m.getReturnType().equals(ValueStatistic.class)) {
            ValueStatistic stat = (ValueStatistic) m.invoke(extendedStatistics, new Object[0]);
            if (stat != null) {
                recordValueStatistic(proxies, stat, "cache.", m);
            }
        } else if (m.getReturnType().equals(ExtendedStatistics.Statistic.class)) {
            Statistic<Number> stat = (Statistic<Number>) m.invoke(extendedStatistics, new Object[0]);
            if (stat != null) {
                recordStatistic(proxies, stat, "cache.", m);
            }
        } else if (m.getReturnType().equals(ExtendedStatistics.Result.class)) {
            Result res = (Result) m.invoke(extendedStatistics, new Object[0]);
            if (res != null) {
                recordResult(proxies, res, m.getName());
            }
        } else if (m.getReturnType().equals(ExtendedStatistics.Operation.class)) {
            Operation op = (Operation) m.invoke(extendedStatistics, new Object[0]);
            if (op.type() != null && op.type().isEnum() && op.type().getEnumConstants() != null) {
                recordOperation(proxies, extendedStatistics, m.getName(), op);
            }
        }
    }

    private void recordStatistic(List<AttributeProxy> proxies, final Statistic<Number> stat, String prefix, Method m) {
        String name = m.getName();
        if (name.startsWith("get")) {
            name = name.substring("get".length());
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        name = prefix + name;
        AttributeProxy<Object> proxy = new AttributeProxy<Object>(Object.class, name, name, true, false) {

            @Override
            public Object get(String name) {
                return stat.value();
            }

        };
        proxies.add(proxy);
    }

    private void recordValueStatistic(List<AttributeProxy> proxies, final ValueStatistic stat, String prefix, Method m) {
        String name = m.getName();
        if (name.startsWith("get")) {
            name = name.substring("get".length());
            name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
        }
        name = prefix + name;
        AttributeProxy<Object> proxy = new AttributeProxy<Object>(Object.class, name, name, true, false) {

            @Override
            public Object get(String name) {
                return stat.value();
            }

        };
        proxies.add(proxy);

    }

    private void recordOperation(List<AttributeProxy> proxies, ExtendedStatistics extendedStatistics, String name, final Operation op) {
        String smallName = name;

        AttributeProxy proxy = new BooleanBeanProxy(smallName + ".alwaysOn", "Set this operation statistic always on/off", true, true) {
            @Override
            public void set(String name, Boolean t) {
                op.setAlwaysOn(t.booleanValue());
            }

            @Override
            public Boolean get(String name) {
                return op.isAlwaysOn();
            }

        };
        proxies.add(proxy);

        proxy = new LongBeanProxy(smallName + ".sampleWindow", "Sampling window size, nanoseconds", true, true) {
            @Override
            public void set(String name, Long t) {
                op.setWindow(t.longValue(), TimeUnit.NANOSECONDS);
            }

            @Override
            public Long get(String name) {
                return op.getWindowSize(TimeUnit.NANOSECONDS);
            }

        };
        proxies.add(proxy);

        proxy = new LongBeanProxy(smallName + ".sampleHistoryCapacity", "Sampling history capacity", true, true) {
            @Override
            public void set(String name, Long t) {
                op.setHistory(t.intValue(), op.getHistorySampleTime(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            }

            @Override
            public Long get(String name) {
                return (long) op.getHistorySampleSize();
            }

        };
        proxies.add(proxy);

        proxy = new LongBeanProxy(smallName + ".sampleHistoryTime", "Sampling history capacity", true, true) {
            @Override
            public void set(String name, Long t) {
                op.setHistory(t.intValue(), op.getHistorySampleTime(TimeUnit.NANOSECONDS), TimeUnit.NANOSECONDS);
            }

            @Override
            public Long get(String name) {
                return op.getHistorySampleTime(TimeUnit.NANOSECONDS);
            }

        };
        proxies.add(proxy);

        /*
         * TBD. These will need to be proxied operations.Worry about them later.
         * 
         * op.setHistory(samples, time, unit)
         * op.setWindow(time, unit);
         */

        for (Object t : op.type().getEnumConstants()) {
            String camelCase = t.toString().toLowerCase();
            camelCase = (Character.toUpperCase(camelCase.charAt(0))) + camelCase.substring(1);
            recordResult(proxies, op.component((Enum) t), smallName + "." + camelCase);
        }
    }

    private void recordResult(List<AttributeProxy> proxies, final Result result, String longerName) {
        recordLongStatistic(proxies, longerName + ".count", "Statistic Counter", result.count());
        recordDoubleStatistic(proxies, longerName + ".rate", "Statistic Rate", result.rate());
        recordLongStatistic(proxies, longerName + ".latencyMin", "Statistic Latency Minimum", result.latency().minimum());
        recordLongStatistic(proxies, longerName + ".latencyMax", "Statistic Latency Maximum", result.latency().maximum());
        recordDoubleStatistic(proxies, longerName + ".latencyAvg", "Statistic Latency Average", result.latency().average());
    }

    /**
     * Record double statistic.
     * 
     * @param proxies the proxies
     * @param longerName the longer name
     * @param baseDescription the base description
     * @param stat the stat
     */
    public void recordDoubleStatistic(List<AttributeProxy> proxies, String longerName, String baseDescription, final Statistic<Double> stat) {

        AttributeProxy proxy;

        // .active();
        proxy = new BooleanBeanProxy(longerName + "Active", baseDescription + " active?", true, false) {

            @Override
            public Boolean get(String name) {
                return stat.active();
            }
        };
        proxies.add(proxy);

        // .value();
        proxy = new DoubleBeanProxy(longerName, baseDescription, true, false) {

            @Override
            public Double get(String name) {
                return stat.value();
            }

        };
        proxies.add(proxy);

        // .history();
        proxy = new AttributeProxy<Map>(Map.class, longerName + "History", baseDescription + " History", true, false) {

            @Override
            public Map get(String name) {
                if (stat.active()) {
                    return historyToMapDouble(stat.history());
                }
                return Collections.EMPTY_MAP;
            }
        };
        proxies.add(proxy);
    }

    /**
     * Record long statistic.
     * 
     * @param proxies the proxies
     * @param longerName the longer name
     * @param baseDescription the base description
     * @param stat the stat
     */
    public void recordLongStatistic(List<AttributeProxy> proxies, String longerName, String baseDescription, final Statistic<Long> stat) {
        AttributeProxy proxy;

        proxy = new BooleanBeanProxy(longerName + "Active", baseDescription + " active?", true, false) {

            @Override
            public Boolean get(String name) {
                return stat.active();
            }
        };
        proxies.add(proxy);

        proxy = new LongBeanProxy(longerName, baseDescription, true, false) {

            @Override
            public Long get(String name) {
                return stat.value();
            }

        };
        proxies.add(proxy);

        proxy = new AttributeProxy<Map>(Map.class, longerName + "History", baseDescription + " History", true, false) {

            @Override
            public Map get(String name) {
                if (stat.active()) {
                    return historyToMapLong(stat.history());
                }
                return Collections.EMPTY_MAP;
            }
        };
        proxies.add(proxy);
    }

    /**
     * History to map long.
     * 
     * @param history the history
     * @return the map
     */
    protected Map historyToMapLong(List<Timestamped<Long>> history) {
        TreeMap<Long, Long> map = new TreeMap<Long, Long>();
        for (Timestamped<Long> ts : history) {
            map.put(ts.getTimestamp(), ts.getSample());
        }
        return map;
    }

    /**
     * History to map double.
     * 
     * @param history the history
     * @return the map
     */
    protected Map historyToMapDouble(List<Timestamped<Double>> history) {
        TreeMap<Long, Double> map = new TreeMap<Long, Double>();
        for (Timestamped<Double> ts : history) {
            map.put(ts.getTimestamp(), ts.getSample());
        }
        return map;
    }

    /**
     * Divine the name.
     * 
     * @param cache the cache
     * @return the string
     */
    public static String divineName(Ehcache cache) {
        return cache.getCacheManager().getName() + "." + cache.getName();
    }
}
