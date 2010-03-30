package net.sf.ehcache.store.compound;

/**
 * @author Alex Snaps
 */
public interface CopyStrategy {
    /**
     * Deep copies some object and returns the copy
     * @param value the value to copy
     * @param <T> type
     * @return the copy
     */
    <T> T copy(final T value);
}
