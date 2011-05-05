package net.sf.ehcache.xmemcached;

import com.google.code.yanf4j.config.Configuration;
import com.google.code.yanf4j.core.SocketOption;
import net.rubyeye.xmemcached.CommandFactory;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.MemcachedClientStateListener;
import net.rubyeye.xmemcached.MemcachedSessionLocator;
import net.rubyeye.xmemcached.auth.AuthInfo;
import net.rubyeye.xmemcached.buffer.BufferAllocator;
import net.rubyeye.xmemcached.buffer.SimpleBufferAllocator;
import net.rubyeye.xmemcached.impl.ArrayMemcachedSessionLocator;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import net.rubyeye.xmemcached.transcoders.Transcoder;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class EhcacheXMemcachedClientBuilder implements MemcachedClientBuilder {

    private MemcachedSessionLocator sessionLocator = new ArrayMemcachedSessionLocator();
    private BufferAllocator bufferAllocator = new SimpleBufferAllocator();
    private Configuration configuration = getDefaultConfiguration();
    private Transcoder transcoder = new SerializingTranscoder();


    private final CacheManager cacheManager;
    private final Ehcache cache;

    public EhcacheXMemcachedClientBuilder(CacheManager cacheManager, String cacheName) {
        this.cacheManager = cacheManager;
        this.cache = cacheManager.getEhcache(cacheName);
    }

    public MemcachedSessionLocator getSessionLocator() {
        return sessionLocator;
    }

    public void setSessionLocator(MemcachedSessionLocator sessionLocator) {
        this.sessionLocator = sessionLocator;
    }

    public BufferAllocator getBufferAllocator() {
        return bufferAllocator;
    }

    public void setBufferAllocator(BufferAllocator bufferAllocator) {
        this.bufferAllocator = bufferAllocator;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public static final Configuration getDefaultConfiguration() {
        final Configuration configuration = new Configuration();
        configuration
                .setSessionReadBufferSize(MemcachedClient.DEFAULT_SESSION_READ_BUFF_SIZE);
        configuration
                .setReadThreadCount(MemcachedClient.DEFAULT_READ_THREAD_COUNT);
        configuration
                .setSessionIdleTimeout(MemcachedClient.DEFAULT_SESSION_IDLE_TIMEOUT);
        configuration.setWriteThreadCount(0);
        return configuration;
    }


    public MemcachedClient build() throws IOException {
        return new EhcacheXMemcachedClient(cache, transcoder);
    }

    public void setConnectionPoolSize(int poolSize) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Transcoder getTranscoder() {
        return transcoder;
    }

    public void setTranscoder(Transcoder transcoder) {
        this.transcoder = transcoder;
    }

    public CommandFactory getCommandFactory() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addStateListener(MemcachedClientStateListener stateListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeStateListener(MemcachedClientStateListener stateListener) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setStateListeners(List<MemcachedClientStateListener> stateListeners) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setCommandFactory(CommandFactory commandFactory) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setSocketOption(SocketOption socketOption, Object value) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<SocketOption, Object> getSocketOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setAuthInfoMap(Map<InetSocketAddress, AuthInfo> map) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public Map<InetSocketAddress, AuthInfo> getAuthInfoMap() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void addAuthInfo(InetSocketAddress address, AuthInfo authInfo) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void removeAuthInfo(InetSocketAddress address) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public String getName() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setName(String name) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public void setFailureMode(boolean failureMode) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public boolean isFailureMode() {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
