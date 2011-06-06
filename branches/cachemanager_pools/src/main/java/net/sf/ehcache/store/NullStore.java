package net.sf.ehcache.store;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.writer.CacheWriterManager;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class NullStore extends AbstractStore {
    public boolean put(Element element) throws CacheException {
        return false;
    }

    public boolean putWithWriter(Element element, CacheWriterManager writerManager) throws CacheException {
        return false;
    }

    public Element get(Object key) {
        return null;
    }

    public Element getQuiet(Object key) {
        return null;
    }

    public List getKeys() {
        return Collections.emptyList();
    }

    public Element remove(Object key) {
        return null;
    }

    public Element removeWithWriter(Object key, CacheWriterManager writerManager) throws CacheException {
        return null;
    }

    public void removeAll() throws CacheException {
    }

    public Element putIfAbsent(Element element) throws NullPointerException {
        return null;
    }

    public Element removeElement(Element element, ElementValueComparator comparator) throws NullPointerException {
        return null;
    }

    public boolean replace(Element old, Element element, ElementValueComparator comparator) throws NullPointerException, IllegalArgumentException {
        return false;
    }

    public Element replace(Element element) throws NullPointerException {
        return null;
    }

    public void dispose() {
    }

    public int getSize() {
        return 0;
    }

    public int getInMemorySize() {
        return 0;
    }

    public int getOffHeapSize() {
        return 0;
    }

    public int getOnDiskSize() {
        return 0;
    }

    public int getTerracottaClusteredSize() {
        return 0;
    }

    public long getInMemorySizeInBytes() {
        return 0;
    }

    public long getOffHeapSizeInBytes() {
        return 0;
    }

    public long getOnDiskSizeInBytes() {
        return 0;
    }

    public Status getStatus() {
        return null;
    }

    public boolean containsKey(Object key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean containsKeyOnDisk(Object key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean containsKeyOffHeap(Object key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean containsKeyInMemory(Object key) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void expireElements() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void flush() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean bufferFull() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Policy getInMemoryEvictionPolicy() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setInMemoryEvictionPolicy(Policy policy) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getInternalContext() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Object getMBean() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
