/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.agent.loader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Util {
  public static byte[] extract(InputStream in) throws IOException {
    if (in == null) throw new NullPointerException();

    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      byte[] data = new byte[4096];
      int read = 0;
      while ((read = in.read(data, 0, data.length)) > 0) {
        out.write(data, 0, read);
      }
      return out.toByteArray();
    } finally {
      try {
        in.close();
      } catch (IOException ioe) {
        //
      }
    }
  }
}
