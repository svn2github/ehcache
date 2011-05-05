package net.sf.ehcache.memcached;

import com.thimbleware.jmemcached.Key;
import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author Ludovic Orban
 */
public class EhcacheKey implements Serializable {

    private final byte[] bytes;

    public EhcacheKey(Key key) {
        this.bytes = key.bytes.array();
    }

    public Key getKey() {
        return new Key(new BigEndianHeapChannelBuffer(bytes));
    }

    @Override
    public boolean equals(Object obj) {
        if (!EhcacheKey.class.equals(obj.getClass())) {
            return false;
        }
        EhcacheKey otherKey = (EhcacheKey) obj;

        return Arrays.equals(bytes, otherKey.bytes);
    }

    public Object toKey() {
        try {
            return new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return this;
        }
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        try {
            return "EhcacheKey [" + new String(bytes, "utf-8") + "]";
        } catch (UnsupportedEncodingException e) {
            return "EhcacheKey [#binary#]";
        }
    }

}
