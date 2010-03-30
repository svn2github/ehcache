package net.sf.ehcache.store.compound;

import net.sf.ehcache.CacheException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * A copy strategy that uses Serialization to copy the object graph
 * @author Alex Snaps
 */
public class SerializationCopyStrategy implements CopyStrategy {

    /**
     * @inheritDoc
     */
    public <T> T copy(final T value) {
        final T newValue;
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        ObjectOutputStream oos  = null;
        ObjectInputStream ois  = null;
        try {
                oos = new ObjectOutputStream(bout);
                oos.writeObject(value);
                ByteArrayInputStream bin = new ByteArrayInputStream(bout.toByteArray());
                ois = new ObjectInputStream(bin);
                newValue = (T)ois.readObject();
            } catch (Exception e) {
                throw new CacheException("When configured copyOnRead or copyOnWrite, a Store will only accept Serializable values", e);
            } finally {
                try {
                    if (oos != null) {
                        oos.close();
                    }
                    if (ois != null) {
                        ois.close();
                    }
                } catch (Exception e) {
                }
            }
        return newValue;
    }
}
