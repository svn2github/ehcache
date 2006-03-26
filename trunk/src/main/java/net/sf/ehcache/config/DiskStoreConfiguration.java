/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2003 - 2004 Greg Luck.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by Greg Luck
 *       (http://sourceforge.net/users/gregluck) and contributors.
 *       See http://sourceforge.net/project/memberlist.php?group_id=93232
 *       for a list of contributors"
 *    Alternately, this acknowledgement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "EHCache" must not be used to endorse or promote products
 *    derived from this software without prior written permission. For written
 *    permission, please contact Greg Luck (gregluck at users.sourceforge.net).
 *
 * 5. Products derived from this software may not be called "EHCache"
 *    nor may "EHCache" appear in their names without prior written
 *    permission of Greg Luck.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL GREG LUCK OR OTHER
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by contributors
 * individuals on behalf of the EHCache project.  For more
 * information on EHCache, please see <http://ehcache.sourceforge.net/>.
 *
 */
package net.sf.ehcache.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A class to represent DiskStore configuration
 * e.g. <diskStore path="java.io.tmpdir" />
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id: DiskStoreConfiguration.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public class DiskStoreConfiguration {
    private static final Log LOG = LogFactory.getLog(DiskStoreConfiguration.class.getName());

    private String path;

    /**
     * The diskStore path
     */
    public String getPath() {
        return path;
    }

    /**
     * Translates and sets the path.
     *
     * @param path If the path contains a Java System Property it is replaced by
     *             its value in the running VM. The following properties are translated:
     *             <ul>
     *             <li><code>user.home</code> - User's home directory
     *             <li><code>user.dir</code> - User's current working directory
     *             <li><code>java.io.tmpdir</code> - Default temp file path
     *             </ul>
     *             e.g. <code>java.io/tmpdir/caches</code> might become <code>/tmp/caches</code>
     */
    public void setPath(final String path) {
        /** A constants class with method scope */
        class Env {
            static final String USER_HOME = "user.home";
            static final String USER_DIR = "user.dir";
            static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
        }

        String translatedPath = replaceToken(Env.USER_HOME, System.getProperty(Env.USER_HOME), path);
        translatedPath = replaceToken(Env.USER_DIR, System.getProperty(Env.USER_DIR), translatedPath);
        translatedPath = replaceToken(Env.JAVA_IO_TMPDIR, System.getProperty(Env.JAVA_IO_TMPDIR), translatedPath);

        if (LOG.isDebugEnabled()) {
            LOG.debug("Disk Store Path: " + translatedPath);
        }
        this.path = translatedPath;
    }

    private String replaceToken(final String token, final String replacement, final String source) {
        int foundIndex = source.indexOf(token);
        if (foundIndex == -1) {
            return source;
        } else {
            String firstFragment = source.substring(0, foundIndex);
            String lastFragment = source.substring(foundIndex + token.length(), source.length());
            return new StringBuffer()
                    .append(firstFragment)
                    .append(replacement)
                    .append(lastFragment)
                    .toString();
        }
    }

}
