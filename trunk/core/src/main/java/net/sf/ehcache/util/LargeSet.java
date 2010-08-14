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
