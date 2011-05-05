package net.sf.ehcache.xmemcached;

import net.rubyeye.xmemcached.CASOperation;
import net.rubyeye.xmemcached.Counter;
import net.rubyeye.xmemcached.GetsResponse;
import net.rubyeye.xmemcached.KeyIterator;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.impl.ReconnectRequest;
import net.rubyeye.xmemcached.networking.Connector;
import net.rubyeye.xmemcached.transcoders.CachedData;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.rubyeye.xmemcached.utils.Protocol;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

/**
 * @author Ludovic Orban
 */
public class EhcacheXMemcachedClient implements MemcachedClient {
    private final Ehcache cache;
    private Transcoder defaultTranscoder;

    public EhcacheXMemcachedClient(Ehcache cache, Transcoder defaultTranscoder) {
        this.cache = cache;
        this.defaultTranscoder = defaultTranscoder;
    }

    public void setMergeFactor(int mergeFactor) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public long getConnectTimeout() {
        return 0;
    }

    public void setConnectTimeout(long connectTimeout) {
    }

    public Connector getConnector() {
        return null;
    }

    public void setOptimizeGet(boolean optimizeGet) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setOptimizeMergeBuffer(boolean optimizeMergeBuffer) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isShutdown() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addServer(String server, int port) throws IOException {
    }

    public void addServer(InetSocketAddress inetSocketAddress) throws IOException {
    }

    public void addServer(String hostList) throws IOException {
    }

    public List<String> getServersDescription() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeServer(String hostList) {
    }

    public void setBufferAllocator(BufferAllocator bufferAllocator) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> T get(String key, long timeout, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        Element element = cache.get(key);
        if (element == null) {
            return null;
        }
        CachedDataWrapper cachedDataWrapper = (CachedDataWrapper) element.getObjectValue();
        return transcoder.decode(cachedDataWrapper.getCachedData());
    }

    public <T> T get(String key, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return (T) get(key, timeout, defaultTranscoder);
    }

    public <T> T get(String key, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return get(key, 0, transcoder);
    }

    public <T> T get(String key) throws TimeoutException, InterruptedException, MemcachedException {
        return (T) get(key, 0, defaultTranscoder);
    }

    public <T> GetsResponse<T> gets(String key, long timeout, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> GetsResponse<T> gets(String key) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> GetsResponse<T> gets(String key, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> GetsResponse<T> gets(String key, Transcoder transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, T> get(Collection<String> keyCollections, long timeout, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, T> get(Collection<String> keyCollections, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, T> get(Collection<String> keyCollections) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, T> get(Collection<String> keyCollections, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, GetsResponse<T>> gets(Collection<String> keyCollections, long timeout, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, GetsResponse<T>> gets(Collection<String> keyCollections) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, GetsResponse<T>> gets(Collection<String> keyCollections, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> Map<String, GetsResponse<T>> gets(Collection<String> keyCollections, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean set(String key, int exp, T value, Transcoder<T> transcoder, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        CachedDataWrapper encoded = new CachedDataWrapper(transcoder.encode(value));
        Element element = new Element(key, encoded);
        if (exp > 0) {
            element.setTimeToLive(exp);
        }
        cache.put(element);
        return true;
    }

    public boolean set(String key, int exp, Object value) throws TimeoutException, InterruptedException, MemcachedException {
        return set(key, exp, value, defaultTranscoder, 0);
    }

    public boolean set(String key, int exp, Object value, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return set(key, exp, value, defaultTranscoder, timeout);
    }

    public <T> boolean set(String key, int exp, T value, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return set(key, exp, value, defaultTranscoder, 0);
    }

    public void setWithNoReply(String key, int exp, Object value) throws InterruptedException, MemcachedException {
        setWithNoReply(key, exp, value, defaultTranscoder);
    }

    public <T> void setWithNoReply(String key, int exp, T value, Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
        Element element = new Element(key, transcoder.encode(value));
        if (exp > 0) {
            element.setTimeToLive(exp);
        }
        cache.put(element);
    }

    public <T> boolean add(String key, int exp, T value, Transcoder<T> transcoder, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        CachedDataWrapper encoded = new CachedDataWrapper(transcoder.encode(value));
        Element element = new Element(key, encoded);
        if (exp > 0) {
            element.setTimeToLive(exp);
        }
        return cache.putIfAbsent(element) == null;
    }

    public boolean add(String key, int exp, Object value) throws TimeoutException, InterruptedException, MemcachedException {
        return add(key, exp, value, defaultTranscoder, 0);
    }

    public boolean add(String key, int exp, Object value, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return add(key, exp, value, defaultTranscoder, timeout);
    }

    public <T> boolean add(String key, int exp, T value, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return add(key, exp, value, transcoder, 0);
    }

    public void addWithNoReply(String key, int exp, Object value) throws InterruptedException, MemcachedException {
        addWithNoReply(key, exp, value, defaultTranscoder);
    }

    public <T> void addWithNoReply(String key, int exp, T value, Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
        CachedDataWrapper encoded = new CachedDataWrapper(transcoder.encode(value));
        Element element = new Element(key, encoded);
        if (exp > 0) {
            element.setTimeToLive(exp);
        }
        cache.putIfAbsent(element);
    }

    public <T> boolean replace(String key, int exp, T value, Transcoder<T> transcoder, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean replace(String key, int exp, Object value) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean replace(String key, int exp, Object value, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean replace(String key, int exp, T value, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void replaceWithNoReply(String key, int exp, Object value) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> void replaceWithNoReply(String key, int exp, T value, Transcoder<T> transcoder) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean append(String key, Object value) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean append(String key, Object value, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void appendWithNoReply(String key, Object value) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean prepend(String key, Object value) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean prepend(String key, Object value, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void prependWithNoReply(String key, Object value) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean cas(String key, int exp, Object value, long cas) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, T value, Transcoder<T> transcoder, long timeout, long cas) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean cas(String key, int exp, Object value, long timeout, long cas) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, T value, Transcoder<T> transcoder, long cas) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, CASOperation<T> operation, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, GetsResponse<T> getsReponse, CASOperation<T> operation, Transcoder<T> transcoder) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, GetsResponse<T> getsReponse, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, GetsResponse<T> getsResponse, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, int exp, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> boolean cas(String key, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> void casWithNoReply(String key, GetsResponse<T> getsResponse, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> void casWithNoReply(String key, int exp, GetsResponse<T> getsReponse, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> void casWithNoReply(String key, int exp, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public <T> void casWithNoReply(String key, CASOperation<T> operation) throws TimeoutException, InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean delete(String key, int time) throws TimeoutException, InterruptedException, MemcachedException {
        cache.remove(key);
        return true;
    }

    public boolean delete(String key, long opTimeout) throws TimeoutException, InterruptedException, MemcachedException {
        cache.remove(key);
        return true;
    }

    public Map<InetSocketAddress, String> getVersions() throws TimeoutException, InterruptedException, MemcachedException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long incr(String key, long delta) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long incr(String key, long delta, long initValue) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long incr(String key, long delta, long initValue, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long decr(String key, long delta) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long decr(String key, long delta, long initValue) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long decr(String key, long delta, long initValue, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void flushAll() throws TimeoutException, InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public void flushAllWithNoReply() throws InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public void flushAll(long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public void flushAll(InetSocketAddress address) throws MemcachedException, InterruptedException, TimeoutException {
        cache.removeAll();
    }

    public void flushAllWithNoReply(InetSocketAddress address) throws MemcachedException, InterruptedException {
        cache.removeAll();
    }

    public void flushAll(InetSocketAddress address, long timeout) throws MemcachedException, InterruptedException, TimeoutException {
        cache.removeAll();
    }

    public void flushAll(String host) throws TimeoutException, InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public Map<String, String> stats(InetSocketAddress address) throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<String, String> stats(InetSocketAddress address, long timeout) throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<InetSocketAddress, Map<String, String>> getStats(long timeout) throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<InetSocketAddress, Map<String, String>> getStats() throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<InetSocketAddress, Map<String, String>> getStatsByItem(String itemName) throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void shutdown() throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean delete(String key) throws TimeoutException, InterruptedException, MemcachedException {
        cache.remove(key);
        return true;
    }

    public Transcoder getTranscoder() {
        return defaultTranscoder;
    }

    public void setTranscoder(Transcoder transcoder) {
        this.defaultTranscoder = transcoder;
    }

    public Map<InetSocketAddress, Map<String, String>> getStatsByItem(String itemName, long timeout) throws MemcachedException, InterruptedException, TimeoutException {
        return null;
    }

    public long getOpTimeout() {
        return 0;
    }

    public void setOpTimeout(long opTimeout) {
    }

    public Map<InetSocketAddress, String> getVersions(long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        return null;
    }

    public Collection<InetSocketAddress> getAvaliableServers() {
        return null;
    }

    public void addServer(String server, int port, int weight) throws IOException {
    }

    public void addServer(InetSocketAddress inetSocketAddress, int weight) throws IOException {
    }

    public void deleteWithNoReply(String key, int time) throws InterruptedException, MemcachedException {
        cache.remove(key);
    }

    public void deleteWithNoReply(String key) throws InterruptedException, MemcachedException {
        cache.remove(key);
    }

    public void incrWithNoReply(String key, long delta) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void decrWithNoReply(String key, long delta) throws InterruptedException, MemcachedException {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setLoggingLevelVerbosity(InetSocketAddress address, int level) throws TimeoutException, InterruptedException, MemcachedException {
    }

    public void setLoggingLevelVerbosityWithNoReply(InetSocketAddress address, int level) throws InterruptedException, MemcachedException {
    }

    public void addStateListener(MemcachedClientStateListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeStateListener(MemcachedClientStateListener listener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Collection<MemcachedClientStateListener> getStateListeners() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void flushAllWithNoReply(int exptime) throws InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public void flushAll(int exptime, long timeout) throws TimeoutException, InterruptedException, MemcachedException {
        cache.removeAll();
    }

    public void flushAllWithNoReply(InetSocketAddress address, int exptime) throws MemcachedException, InterruptedException {
        cache.removeAll();
    }

    public void flushAll(InetSocketAddress address, long timeout, int exptime) throws MemcachedException, InterruptedException, TimeoutException {
        cache.removeAll();
    }

    public void setHealSessionInterval(long healConnectionInterval) {
    }

    public long getHealSessionInterval() {
        return 0;
    }

    public Protocol getProtocol() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setPrimitiveAsString(boolean primitiveAsString) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setConnectionPoolSize(int poolSize) {
    }

    public void setEnableHeartBeat(boolean enableHeartBeat) {
    }

    public void setSanitizeKeys(boolean sanitizeKey) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isSanitizeKeys() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Counter getCounter(String key) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public Counter getCounter(String key, long initialValue) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public KeyIterator getKeyIterator(InetSocketAddress address) throws MemcachedException, InterruptedException, TimeoutException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> map) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long decr(String key, long delta, long initValue, long timeout, int exp) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public long incr(String key, long delta, long initValue, long timeout, int exp) throws TimeoutException, InterruptedException, MemcachedException {
        return 0;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return cache.getName();
    }

    public void setName(String name) {
    }

    public Queue<ReconnectRequest> getReconnectRequestQueue() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFailureMode(boolean failureMode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isFailureMode() {
        return false;
    }
}
