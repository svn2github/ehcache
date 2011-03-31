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

package net.sf.ehcache.store.compound;

import net.sf.ehcache.Element;

/**
 * IdentityElementProxyFactory is implemented by all non-proxying factories.
 * <p>
 * Non-proxying factories are used to implement direct in-heap storage of Elements.
 * Their encode methods will typically (but not necessarily) return the same element
 * as was passed to them.  They may however choose to perform eviction operations either
 * synchronously within their methods, or trigger asynchronous evictions from them.
 * 
 * @author Chris Dennis
 */
public interface IdentityElementSubstituteFactory extends InternalElementSubstituteFactory<Element> {
    //
}
