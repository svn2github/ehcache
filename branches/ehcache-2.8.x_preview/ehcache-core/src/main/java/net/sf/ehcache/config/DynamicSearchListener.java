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

package net.sf.ehcache.config;

import net.sf.ehcache.search.attribute.DynamicAttributesExtractor;

/**
 * Listener for changes to dynamic attributes extractor config
 * @author vfunshte
 */
public interface DynamicSearchListener {
    /**
     * Called to indicate that a new dynamic attributes extractor was added
     * @param oldValue
     * @param newValue
     */
    public void extractorChanged(DynamicAttributesExtractor oldValue, DynamicAttributesExtractor newValue);
}
