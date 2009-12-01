/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.agent.loader;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32InputArchive;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.AllPermission;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.security.cert.Certificate;

public class Jar {
  // this class synchronized for many reasons:
  // - to avoid concurrent inflation
  // - update to access time that will be observed by other threads
  // - the underlying archive itself is it not thread safe!

  private static final long      NOT_INITIALIZED = -1;

  private final URL              source;
  private final JarManager       jarManager;
  private final ProtectionDomain pd;

  private long                   lastAccess      = NOT_INITIALIZED;
  private Zip32InputArchive      archive;
  private byte[]                 contents;

  Jar(URL source, JarManager jarManager) {
    this.source = source;
    this.jarManager = jarManager;

    CodeSource cs = new CodeSource(source, new Certificate[] {});
    this.pd = new ProtectionDomain(cs, new AllPermission().newPermissionCollection());
  }

  public URL getSource() {
    return source;
  }

  synchronized boolean isDeflated() {
    return archive == null && contents == null;
  }

  synchronized void deflateIfIdle(long idle) {
    if (lastAccess == NOT_INITIALIZED || archive == null) return;

    if ((System.currentTimeMillis() - lastAccess) > idle) {
      try {
        archive.close();
      } catch (IOException e) {
        // ignore
      }

      contents = null;
      archive = null;
    }
  }

  ProtectionDomain getProtectionDomain() {
    return this.pd;
  }

  public synchronized boolean hasResource(String res) throws IOException {
    touch();
    return archive.getArchiveEntry(res) != null;
  }

  private void touch() throws IOException {
    this.lastAccess = System.currentTimeMillis();
    inflateIfNeeded();
  }

  public synchronized byte[] lookup(String resource) throws IOException {
    touch();
    InputStream in = archive.getInputStream(resource);
    if (in == null) { return null; }
    return Util.extract(in);
  }

  public synchronized byte[] contents() throws IOException {
    touch();
    return contents;
  }

  private void inflateIfNeeded() throws IOException {
    if (archive == null) {
      // handle multilevel jar in jars
      String extForm = source.toExternalForm();
      if (extForm.startsWith("jar:jar:")) {
        int nesting = getNumJarSeparators(extForm);
        if (nesting > 2) {
          // cannot handle more than 3 levels of nesting
          throw new IOException("Cannot handle more than 3 levels of nested jar lookups");
        } else if (nesting <= 0) { throw new MalformedURLException("No '!/' found in URL beginning with 'jar:'"); }
        extForm = extForm.substring("jar:".length());
        String secondJarUrl = extForm.substring(0, extForm.lastIndexOf("!/"));
        String resourceInSecondJar = extForm.substring(extForm.lastIndexOf("!/") + "!/".length());
        if (resourceInSecondJar.startsWith("/")) {
          resourceInSecondJar = resourceInSecondJar.substring(1);
        }
        Jar secondJar = jarManager.getOrCreate(secondJarUrl, new URL(secondJarUrl));
        contents = secondJar.lookup(resourceInSecondJar);
      } else {
        contents = Util.extract(source.openStream());
      }
      archive = new Zip32InputArchive(new ByteArrayReadOnlyFile(contents), "UTF-8", false, false);
      jarManager.jarOpened(this);
    }
  }

  private static int getNumJarSeparators(String str) {
    int rv = 0;
    int length = str.length();
    for (int i = 0; i < length; i++) {
      char ch = str.charAt(i);
      if (ch == '!' && i < length - 1 && str.charAt(i + 1) == '/') {
        rv++;
      }
    }
    return rv;
  }

  @Override
  public String toString() {
    return source.toExternalForm();
  }

}