/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package org.terracotta.agent.loader;

import org.terracotta.agent.repkg.de.schlichtherle.io.archive.zip.Zip32InputArchive;

import java.io.IOException;
import java.io.InputStream;
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
      jarManager.jarOpened(this);
      contents = Util.extract(source.openStream());
      archive = new Zip32InputArchive(new ByteArrayReadOnlyFile(contents), "UTF-8", false, false);
    }
  }

  @Override
  public String toString() {
    return source.toExternalForm();
  }

}