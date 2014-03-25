/**
 *  Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package net.sf.ehcache.terracotta;

import net.sf.ehcache.util.FindBugsSuppressWarnings;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * XXX: Copy from to satisfy dependencies
 * @author vfunshte
 */
public class SerializationHelper {
  /**
   * String keys which are really serialized objects will have this as their first char This particular value was chosen
   * since it is an invalid character in UTF-16 (http://unicode.org/faq/utf_bom.html#utf16-7)
   */
  private static final char MARKER = 0xFFFE;
  private static final int MASK = 0xFF;

  /**
   * Serialize object to byte array
   * @param obj
   * @return
   */
public static byte[] serialize(Object obj) {
    try {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(baos);
      oos.writeObject(obj);
      oos.close();
      return baos.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("error serializing " + obj, e);
    }
  }

  /**
   * Deserialize
   * @param bytes
   * @return
   */
@FindBugsSuppressWarnings("DMI_INVOKING_TOSTRING_ON_ARRAY")
  public static Object deserialize(byte[] bytes) {
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
      ObjectInputStream ois = new ObjectInputStream(bais);
      Object obj = ois.readObject();
      ois.close();
      return obj;
    } catch (Exception e) {
      throw new RuntimeException("error deserializing " + bytes, e);
    }
  }

  /**
   * Deserialize
 * @param key
 * @return
 * @throws IOException
 * @throws ClassNotFoundException
 */
public static Object deserializeFromString(String key) throws IOException, ClassNotFoundException {
    if (key.length() >= 1 && key.charAt(0) == MARKER) {
      ObjectInputStream ois = new ObjectInputStream(new StringSerializedObjectInputStream(key));
      return ois.readObject();
    }

    return key;
  }

  /**
   * Serialize object to string
 * @param key
 * @return
 * @throws IOException
 */
public static String serializeToString(Object key) throws IOException {
    if (key instanceof String) {
      String stringKey = (String) key;

      // disallow Strings that start with our marker
      if (stringKey.length() >= 1) {
        if (stringKey.charAt(0) == MARKER) {
          //
          throw new IOException("Illegal string key: " + stringKey);
        }

      }

      return stringKey;
    }

    StringSerializedObjectOutputStream out = new StringSerializedObjectOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(out);
    writeStringKey(key, oos);
    oos.close();

    return out.toString();
  }

  private static void writeStringKey(final Object key, final ObjectOutputStream oos) throws IOException {
    oos.writeObject(key);
  }

  /**
 * @author vfunshte
 *
 */
private static class StringSerializedObjectOutputStream extends OutputStream {
    private static final int DEFAULT_SIZE = 16;
    private int    count;
    private char[] buf;

    StringSerializedObjectOutputStream() {
      this(DEFAULT_SIZE);
    }

    StringSerializedObjectOutputStream(int size) {
      size = Math.max(1, size);
      buf = new char[size];

      // always add our marker char
      buf[count++] = MARKER;
    }

    @Override
    public void write(int b) {
      if (count + 1 > buf.length) {
        char[] newbuf = new char[buf.length << 1];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      writeChar(b);
    }

    private void writeChar(int b) {
      // hibyte is always zero since UTF-8 encoding used for String storage in DSO!
      buf[count++] = (char) (b & MASK);
    }

    @Override
    public void write(byte[] b, int off, int len) {
      if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) {
        throw new IndexOutOfBoundsException();
      } else if (len == 0) { return; }
      int newcount = count + len;
      if (newcount > buf.length) {
        char[] newbuf = new char[Math.max(buf.length << 1, newcount)];
        System.arraycopy(buf, 0, newbuf, 0, count);
        buf = newbuf;
      }

      for (int i = 0; i < len; i++) {
        writeChar(b[off + i]);
      }
    }

    @Override
    public String toString() {
      return new String(buf, 0, count);
    }
  }

  /**
 * @author vfunshte
 *
 */
private static class StringSerializedObjectInputStream extends InputStream {
    private final String source;
    private final int    length;
    private int          index;

    StringSerializedObjectInputStream(String source) {
      this.source = source;
      this.length = source.length();

      read();
    }

    @Override
    public int read() {
      if (index == length) {
        // EOF
        return -1;
      }

      return source.charAt(index++) & MASK;
    }
  }
}
