/**
 *  Copyright 2003-2010 Terracotta, Inc.
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

package net.sf.ehcache.search.aggregator;

public class Max<T> implements Aggregator<T> {

    private Comparable max = null;

    public T aggregateResult() {
        return (T) max;

    }

    public void accept(Object input) throws AggregatorException {
        if (input == null) {
            return;
        }

        Comparable next = getComparable(input);

        if (max == null) {
            max = next;
        } else if (next.compareTo(max) > 0) {
            max = next;
        }
    }

    private static Comparable getComparable(Object o) {
        if (o instanceof Comparable) {
            return (Comparable) o;
        }

        throw new AggregatorException("Value is not Comparable: " + o.getClass());
    }

}
