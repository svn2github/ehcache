package net.sf.ehcache.pool.impl;

import net.sf.ehcache.pool.SizeOfEngine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

/**
 * @author Ludovic Orban
 */
public class SerializationSizeOfEngine implements SizeOfEngine {

  public long sizeOf(final Object key, final Object value, final Object container) {
      try {
          ByteArrayOutputStream baos = new ByteArrayOutputStream();
          ObjectOutputStream oos = new ObjectOutputStream(baos);

          oos.writeObject(key);
          int keySize = baos.size();
          baos.reset();

          oos.writeObject(value);
          int valueSize = baos.size();
          baos.reset();

          oos.writeObject(container);
          int containerSize = baos.size() - keySize - valueSize;

          oos.close();
          baos.close();

          return keySize + valueSize + containerSize;
      } catch (IOException e) {
          throw new RuntimeException("error sizing objects with serialization", e);
      }
  }
}
