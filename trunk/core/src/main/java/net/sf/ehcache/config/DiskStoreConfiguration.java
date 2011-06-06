/**
 *  Copyright 2003-2010 Terracotta, Inc.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A class to represent DiskStore configuration
 * e.g. <diskStore path="${java.io.tmpdir}" />
 *
 * @author <a href="mailto:gluck@thoughtworks.com">Greg Luck</a>
 * @version $Id$
 */
public final class DiskStoreConfiguration {

    private static final Pattern PROPERTY_SUBSTITUTION_PATTERN = Pattern.compile("\\$\\{(.+?)\\}");

    private static final Logger LOG = LoggerFactory.getLogger(DiskStoreConfiguration.class.getName());

    /**
     * The path as specified in the config
     */
    private String originalPath;

    /**
     * The path to the directory where .data and .index files will be created.
     */
    private String path;

    /**
     * A constants class for environment variables used in disk store paths
     */
    private static enum Env {
        USER_HOME("user.home"),
        USER_DIR("user.dir"),
        JAVA_IO_TMPDIR("java.io.tmpdir"),
        EHCACHE_DISK_STORE_DIR("ehcache.disk.store.dir");

        private final String variable;

        Env(String variable) {
            this.variable = variable;
        }

        String substitute(String string) {
            String substitution = System.getProperty(variable);
            if (substitution == null) {
                return string;
            } else {
                return string.replaceFirst(Pattern.quote(variable), substitution);
            }
        }
    }

    /**
     * The diskStore path
     */
    public final String getPath() {
        return path;
    }

    /**
     * The diskStore default path, which is the system environment variable
     * available on all Java virtual machines <code>java.io.tmpdir</code>
     */
    public static String getDefaultPath() {
        return Env.JAVA_IO_TMPDIR.substitute(Env.JAVA_IO_TMPDIR.variable);
    }

    /**
     * Builder method to set the disk store path, see {@link #setPath(String)}
     *
     * @return this configuration instance
     */
    public final DiskStoreConfiguration path(final String path) {
        setPath(path);
        return this;
    }

    /**
     * Translates and sets the path.
     * <p>
     * Two forms of path substitution are supported:
     * <ol>
     * <li>To support legacy configurations, four explicit string tokens are replaced with their
     * associated Java system property values.
     * <ul>
     * <li><code>user.home</code> - the user's home directory</li>
     * <li><code>user.dir</code> - the current working directory</li>
     * <li><code>java.io.tmpdir</code> - the default temp file path</li>
     * <li><code>ehcache.disk.store.dir</code> - a system property you would normally specify on the command line, e.g. <code>java -Dehcache.disk.store.dir=/u01/myapp/diskdir</code></li>
     * </ul>
     * </li>
     * <li>These, and all other system properties can also be substituted using the familiar syntax<br>
     * <code>${system-property-name}/some-path-fragment/${other-property-name}</code></li>
     * </ol>
     *
     * @param path disk store path
     */
    public final void setPath(final String path) {
        this.originalPath = path;
        this.path = translatePath(path);
    }

    /**
     * @return the originalPath
     */
    public String getOriginalPath() {
        return originalPath;
    }

    private static String translatePath(String path) {
        String translatedPath = substituteProperties(path);
        for (Env e : Env.values()) {
            translatedPath = e.substitute(translatedPath);
        }
        // Remove duplicate separators: Windows and Solaris
        translatedPath = translatedPath.replace(File.separator + File.separator, File.separator);
        LOG.debug("Disk Store Path: " + translatedPath);
        return translatedPath;
    }

    private static String substituteProperties(String string) {
        Matcher matcher = PROPERTY_SUBSTITUTION_PATTERN.matcher(string);

        StringBuffer eval = new StringBuffer();
        while (matcher.find()) {
            String substitution = System.getProperty(matcher.group(1));
            if (substitution != null) {
                matcher.appendReplacement(eval, Matcher.quoteReplacement(substitution));
            }
        }
        matcher.appendTail(eval);

        return eval.toString();
    }
}
