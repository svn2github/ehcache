/**
 *  Copyright 2003-2009 Terracotta, Inc.
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


import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A class to represent DiskStore configuration
 * e.g. <diskStore path="java.io.tmpdir" />
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class DiskStoreConfiguration {

    private static final Logger LOG = Logger.getLogger(DiskStoreConfiguration.class.getName());


    /**
     * The path to the directory where .data and .index files will be created. 
     */
    private String path;


    /**
     * A constants class for environment variables used in disk store paths
     */
    private static final class Env {

        static final String USER_HOME = "user.home";
        static final String USER_DIR = "user.dir";
        static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
        static final String EHCACHE_DISK_STORE_DIR = "ehcache.disk.store.dir";
    }

    /**
     * The diskStore path
     */
    public final String getPath() {
        return path;
    }

    /**
     * The diskStore default path, which is the system environment variable
     * availablen on all Java virtual machines <code>java.io.tmpdir</code>
     */
    public static String getDefaultPath() {
        return translatePath(Env.JAVA_IO_TMPDIR);
    }

    /**
     * Translates and sets the path.
     *
     * @param path If the path contains a Java System Property token it is replaced by
     *             its value in the running VM. Subdirectories can be specified below the property e.g. java.io.tmpdir/one.
     *  The following properties are translated:
     *             <ul>
     *             <li><code>user.home</code> - User's home directory
     *             <li><code>user.dir</code> - User's current working directory
     *             <li><code>java.io.tmpdir</code> - Default temp file path
     *             <li><code>ehcache.disk.store.di?r</code> - A system property you would normally specify on the command linecan specify with -DDefault temp file path
     *              e.g. <code>java -Dehcache.disk.store.dir=/u01/myapp/diskdir ...</code>
     *             </ul>
     * Additional strings can be placed before and after tokens?
     *             e.g. <code>java.io/tmpdir/caches</code> might become <code>/tmp/caches</code>
     */
    public final void setPath(final String path) {
        String translatedPath = translatePath(path);
        this.path = translatedPath;
    }

    private static String translatePath(String path) {
        String translatedPath = replaceToken(Env.USER_HOME, System.getProperty(Env.USER_HOME), path);
        translatedPath = replaceToken(Env.USER_DIR, System.getProperty(Env.USER_DIR), translatedPath);
        translatedPath = replaceToken(Env.JAVA_IO_TMPDIR, System.getProperty(Env.JAVA_IO_TMPDIR), translatedPath);
        translatedPath = replaceToken(Env.EHCACHE_DISK_STORE_DIR, System.getProperty(Env.EHCACHE_DISK_STORE_DIR), translatedPath);
        //Remove duplicate separators: Windows and Solaris
        translatedPath = replaceToken(File.separator + File.separator, File.separator, translatedPath);


        if (LOG.isLoggable(Level.FINE)) {
            LOG.log(Level.FINE, "Disk Store Path: " + translatedPath);
        }
        return translatedPath;
    }

    /**
     * Replaces a token with replacement text.
     *
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
