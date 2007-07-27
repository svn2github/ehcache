/**
 *  Copyright 2003-2007 Luck Consulting Pty Ltd
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

package net.sf.ehcache.config;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;

/**
 * A class to represent DiskStore configuration
 * e.g. <diskStore path="java.io.tmpdir" />
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class DiskStoreConfiguration {
    private static final Log LOG = LogFactory.getLog(DiskStoreConfiguration.class.getName());

    private String path;

    /**
     * The diskStore path
     */
    public final String getPath() {
        return path;
    }

    /**
     * Translates and sets the path.
     *
     * @param path If the path contains a Java System Property it is replaced by
     *             its value in the running VM. Subdirectories can be specified below the property e.g. java.io.tmpdir/one The following properties are translated:
     *             <ul>
     *             <li><code>user.home</code> - User's home directory
     *             <li><code>user.dir</code> - User's current working directory
     *             <li><code>java.io.tmpdir</code> - Default temp file path
     *             </ul>
     *             e.g. <code>java.io/tmpdir/caches</code> might become <code>/tmp/caches</code>
     */
    public final void setPath(final String path) {
        /** A constants class with method scope */
        final class Env {
            static final String USER_HOME = "user.home";
            static final String USER_DIR = "user.dir";
            static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
        }

        String translatedPath = replaceToken(Env.USER_HOME, System.getProperty(Env.USER_HOME), path);
        translatedPath = replaceToken(Env.USER_DIR, System.getProperty(Env.USER_DIR), translatedPath);
        translatedPath = replaceToken(Env.JAVA_IO_TMPDIR, System.getProperty(Env.JAVA_IO_TMPDIR), translatedPath);
        String separator = File.separator;
        //Remove duplicate separators: Windows and Solaris
        translatedPath = replaceToken(File.separator + File.separator, File.separator, translatedPath);


        if (LOG.isDebugEnabled()) {
            LOG.debug("Disk Store Path: " + translatedPath);
        }
        this.path = translatedPath;
    }

    /**
     * Replaces a token with replacement text.
     * @param token
     * @param replacement
     * @param source
     * @return the String with replacement text applied
     */
    public static String replaceToken(final String token, final String replacement, final String source) {
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
