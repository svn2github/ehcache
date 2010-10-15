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

/**
 * Compute the average (arithmetic mean) as a double
 *
 * @author teck
 */
public class Average implements Aggregator<Double> {

    private long count;
    private double sum;

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: Null values are ignored and not included in the computation
     */
    public void accept(Object input) throws AggregatorException {
        if (input == null) {
            return;
        }

        if (input instanceof Number) {
            count++;
            sum += ((Number) input).doubleValue();
        } else {
            throw new AggregatorException("Non-number type encounted: " + input.getClass());
        }
    }

    /**
     * {@inheritDoc}
     * <p/>
     * NOTE: null is returned if there was no input supplied to this function
     */
    public Double aggregateResult() {
        if (count == 0) {
            return null;
        }

        return sum / count;
    }

}
