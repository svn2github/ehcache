/**
 *  Copyright 2003-2009 Terracotta, Inc.
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

package net.sf.ehcache.config;

/**
 * Class to hold the Terracotta configuration - either a pointer to the real config or a
 * container for embedded config. 
 * 
 * @author amiller@terracotta.org
 */
public class TerracottaConfiguration implements Cloneable {
    /**
     * Represents whether values are stored with serialization in the clustered store
     * or through Terracotta clustered identity.
     * @author amiller
     */
    public enum ValueMode {
        /** When a key or value is put in the cache, serialize the data for sending around the cluster */
        SERIALIZATION,
        
        /** Use Terracotta clustered identity to preserve object identity without serialization */
        IDENTITY;
    }
    
    private boolean clustered = true;
    private ValueMode valueMode = ValueMode.SERIALIZATION;
    private boolean coherentReads = true;

    /**
     * Clones this object, following the usual contract.
     *
     * @return a copy, which independent other than configurations than cannot change.
     * @throws CloneNotSupportedException
     */
    @Override
    public TerracottaConfiguration clone() throws CloneNotSupportedException {
        return (TerracottaConfiguration) super.clone();
    }
    
    /**
     * Used by BeanHandler to set the clustered flag during parsing
     */
    public void setClustered(boolean clustered) {
        this.clustered = clustered;
    }

    /**
     * Check whether clustering is enabled
     */
    public boolean isClustered() {
        return this.clustered;
    }

    /**
     * Used by BeanHandler to set the coherentReads flag during parsing
     */
    public void setCoherentReads(boolean coherentReads) {
        this.coherentReads = coherentReads;
    }

    /**
     * Check whether coherent reads are enabled
     */
    public boolean getCoherentReads() {
        return this.coherentReads;
    }
    /**
     * Used by BeanHandler to set the mode during parsing.  Convert valueMode string to uppercase and 
     * look up enum constant in ValueMode.
     */
    public void setValueMode(String valueMode) {
        if (valueMode == null) {
            throw new IllegalArgumentException("Value mode must be non-null");
        }
        this.valueMode = ValueMode.valueOf(ValueMode.class, valueMode.toUpperCase());
    }

    /**
     * Get the value mode in terms of the mode enum
     */
    public ValueMode getValueMode() {
        return this.valueMode;
    }
}
