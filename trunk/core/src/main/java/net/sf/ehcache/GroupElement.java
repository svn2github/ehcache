package net.sf.ehcache;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * A special cache Element that holds references to group elements via
 * its value which can always relied on being a Map of Set of object keys.
 * This class is intended for use only by Ehcache internally.
 * @author Andrew Liles
 * @author C&eacute;drik LIME
 * @since 2.4
 */
class GroupElement extends Element {

    public static final String MASTER_GROUP_KEY = "net.sf.ehcache.groups.master";

    private static final long serialVersionUID = 66172327024702L;

    GroupElement(Object key) {
        super(key,
                new ConcurrentHashMap<String, Set<Object>>(), //Object value,
                Boolean.TRUE, //Boolean eternal,
                null, //Integer timeToIdleSeconds,
                null); //Integer timeToLiveSeconds);
    }

//    /**
//     * Use with care, GroupElements should only be created by Ehcache
//     * @param key
//     * @param value
//     * @param version
//     * @param creationTime
//     * @param lastAccessTime
//     * @param lastUpdateTime
//     * @param hitCount
//     */
//    GroupElement(Object key, Object value, long version,
//            long creationTime, long lastAccessTime,
//            long lastUpdateTime, long hitCount) {
//        super(key, value, version, creationTime, lastAccessTime,
//                lastUpdateTime, hitCount);
//    }

    /**
     * @return all known groups; key == group key, value == Set of element keys
     */
    public Map<String, Set<Object>> getGroups() {
        return (Map<String, Set<Object>>) getObjectValue();
    }

    public synchronized Set<Object> getGroupMembers(String groupKey) {
        Set<Object> groupMembers = getGroups().get(groupKey);
        if (groupMembers == null) {
            groupMembers = new CopyOnWriteArraySet();//TODO Java 6: Collections.newSetFromMap(new ConcurrentHashMap<Object,Boolean>())
            getGroups().put(groupKey, groupMembers);
        }
        return groupMembers;
    }

    public boolean addMemberToGroup(String groupKey, Object memberKey) {
        return getGroupMembers(groupKey).add(memberKey);
    }

    public boolean removeMemberFromGroup(String groupKey, Object memberKey) {
        Set<Object> groupMembers = getGroups().get(groupKey);
        if (groupMembers == null) {
            return false;
        }
        boolean result = groupMembers.remove(memberKey);
        if (groupMembers.isEmpty()) {
            getGroups().remove(groupKey);
        }
        return result;
    }
}
