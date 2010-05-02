package net.sf.ehcache.store.compound;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Element;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Alex Snaps
 */
public class ImmutableValueElementCopyStrategy implements CopyStrategy {

    CopyStrategy defaultCopyStrategy = new SerializationCopyStrategy();

    public <T> T copy(final T value) {

        final T newValue;
        if (value instanceof Element) {
            Element element = (Element) value;
            Element newElement = new Element(element.getObjectKey(), element.getObjectValue(), element.getVersion(),
                element.getCreationTime(), element.getLastAccessTime(), element.getHitCount(), element.usesCacheDefaultLifespan(),
                element.getTimeToLive(), element.getTimeToIdle(), element.getLastUpdateTime());
            newValue = (T) newElement;
        } else {
            newValue = defaultCopyStrategy.copy(value);
        }

        return newValue;
    }
}
