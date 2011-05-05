package net.sf.ehcache.memcached;

import org.jboss.netty.buffer.BigEndianHeapChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffer;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * @author Ludovic Orban
 */
public class EhcacheValue implements Serializable {

    private final byte[] bytes;

    public EhcacheValue(ChannelBuffer data) {
        this.bytes = data.array();
    }

    public ChannelBuffer getData() {
        return new BigEndianHeapChannelBuffer(bytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (!EhcacheValue.class.equals(obj.getClass())) {
            return false;
        }
        EhcacheValue otherValue = (EhcacheValue) obj;

        return Arrays.equals(bytes, otherValue.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    public Object toValue() {
        try {
            return new String(bytes, "utf-8");
        } catch (UnsupportedEncodingException e) {
            return this;
        }
    }

    @Override
    public String toString() {
        try {
            return "EhcacheValue [" + new String(bytes, "utf-8") + "]";
        } catch (UnsupportedEncodingException e) {
            return "EhcacheValue [#binary#]";
        }
    }
}
