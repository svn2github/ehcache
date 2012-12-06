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

package net.sf.ehcache.statisticsV2;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.ehcache.statisticsV2.Constants.RecordingCost;
import net.sf.ehcache.statisticsV2.Constants.RetrievalCost;

public class EhcacheStatisticsPropertyMap extends HashMap<String,Object>{

    public static final String NAME_PROP = "name";
    public static final String TAGS_PROP = "tags";
    public static final String RETRIEVAL_COST_PROP = "retrieval cost";
    public static final String RECORDING_COST_PROP = "recording cost";

    public EhcacheStatisticsPropertyMap(String name,
            RetrievalCost retrievalCost,
            RecordingCost recordingCost,
            String...tags) {
        super.put(NAME_PROP,name);
        super.put(RECORDING_COST_PROP,recordingCost);
        super.put(RETRIEVAL_COST_PROP,retrievalCost);
        if(tags==null) {
            tags=new String[0];
        }
        super.put(TAGS_PROP,Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(tags))));
    }

    public Set<String> getTags() {
        return (Set<String>) get(TAGS_PROP);
    }

    public RecordingCost getRecordingCost() {
        return (RecordingCost) get(RECORDING_COST_PROP);
    }

    public RetrievalCost getRetrievalCost() {
        return (RetrievalCost) get(RETRIEVAL_COST_PROP);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

}
