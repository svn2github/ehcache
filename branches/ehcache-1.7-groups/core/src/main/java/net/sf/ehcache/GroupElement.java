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
	
	public static final String MASTER_GROUP_KEY = "net.sf.ehcache.groups.master";
	
    private static final long serialVersionUID = 66172327024702L;

	GroupElement(Object key) {
		super(key, 
				makeMemberKeySet(), //Object value, 
				new Boolean(true), //Boolean eternal,
				null, //Integer timeToIdleSeconds, 
				null); //Integer timeToLiveSeconds);
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
