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

package net.sf.ehcache.util;

import java.util.Set;

/**
 * Set for holding large entries of set. The purpose is not to iterate through
 * all entries for add and remove operations.
 * 
 * @author Nabib El-Rahman
 *
 * @param <E>
 */
public abstract class LargeSet<E> extends LargeCollection<E> implements Set<E> {
//
}
