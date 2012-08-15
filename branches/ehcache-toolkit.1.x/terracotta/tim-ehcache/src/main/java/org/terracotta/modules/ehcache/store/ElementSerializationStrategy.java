/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.modules.ehcache.store;

import net.sf.ehcache.AbstractElementData;
import net.sf.ehcache.SerializationModeElementData;

import org.terracotta.cache.serialization.DsoSerializationStrategy3;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ElementSerializationStrategy extends DsoSerializationStrategy3<AbstractElementData> {

  private final boolean compress;

  public ElementSerializationStrategy(boolean compress) {
    this.compress = compress;
  }

  @Override
  public AbstractElementData deserialize(final byte[] data) throws IOException, ClassNotFoundException {
    InputStream in = new ByteArrayInputStream(data);
    if (compress) {
      in = new GZIPInputStream(in);
    }

    OIS ois = new OIS(in, oscSerializer);
    return SerializationModeElementData.create(ois);
  }

  @Override
  public AbstractElementData deserialize(byte[] data, ClassLoader loader) throws IOException, ClassNotFoundException {
    InputStream in = new ByteArrayInputStream(data);
    if (compress) {
      in = new GZIPInputStream(in);
    }
    OIS ois = new OIS(in, oscSerializer, loader);
    return SerializationModeElementData.create(ois);
  }

  @Override
  public byte[] serialize(final AbstractElementData element) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    OutputStream out = baos;
    if (compress) {
      out = new GZIPOutputStream(out);
    }
    OOS oos = new OOS(out, oscSerializer);
    element.write(oos);
    oos.close();
    return baos.toByteArray();
  }
}
