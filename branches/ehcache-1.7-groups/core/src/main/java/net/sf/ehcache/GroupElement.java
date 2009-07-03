/**
 *  Copyright 2003-2008 Luck Consulting Pty Ltd
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

package net.sf.ehcache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


/** A special cache Element that holds references to group elements via
 * its value which can always relied on being a Set of object keys.
 * This class is intended for use only by Ehcache internally.
 * @author Andrew Liles
 * @since 1.7
 *
 */
class GroupElement extends Element {

    /**
     * The default key used to store the Master Group.
     * This is configurable on a per-cache basis, see {@link Cache#setMasterGroupKey(String)}
     */
    public static final String MASTER_GROUP_KEY = "net.sf.ehcache.groups.master";

    private static final long serialVersionUID = 66172327024702L;

    /** Partial constructor intended to make a GroupElement with
     * the special Set Value.  Intended use only by {@link Cache}
     * @param key
     */
    GroupElement(Object key) {
        super(key,
                /* Object value */ makeMemberKeySet(),
                /* Boolean eternal */ Boolean.TRUE,
                /* Integer timeToIdleSeconds */ null,
                /* Integer timeToLiveSeconds */ null);
    }

    /** Use with care, GroupElements should only be created by Ehcache
     * @param key
     * @param value
     * @param version
     * @param creationTime
     * @param lastAccessTime
     * @param nextToLastAccessTime
     * @param lastUpdateTime
     * @param hitCount
     */
    public GroupElement(Object key, Object value, long version,
            long creationTime, long lastAccessTime, long nextToLastAccessTime,
            long lastUpdateTime, long hitCount) {
        super(key, value, version, creationTime, lastAccessTime, nextToLastAccessTime,
                lastUpdateTime, hitCount);
    }

    /** Obtains all the keys of the members of the group.
     * This result may be inaccurate, e.g. an element identified
     * by the key may not longer exist.  The canonical
     * definition of group membership exist within the Element itself,
     * see {@link Element#getGroupKeys()}
     * @return a Set of keys; this Set should never be changed
     */
    public Set getMemberKeys() {
        return (Set) getObjectValue();
    }

    /** Create the value object for a Group Element
     * @return
     */
    private static Set makeMemberKeySet() {
        return Collections.synchronizedSet(new HashSet());
    }

}
