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

package net.sf.ehcache.config.generator;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import net.sf.ehcache.CacheException;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.ConfigurationFactory;

/**
 * Class encapsulating the source of configuration for a cache manager
 *
 * <p />
 *
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 *
 */
public abstract class ConfigurationSource {
    /**
     * protected constructor
     */
    protected ConfigurationSource() {
        //
    }

    /**
     * Utility factory method for creating a {@link ConfigurationSource} based on a file.
     *
     * @param file
     * @return ConfigurationSource for the input file
     */
    public static ConfigurationSource getConfigurationSource(File file) {
        return new FileNameSource(file);
    }

    /**
     * Utility factory method for creating a {@link ConfigurationSource} based on {@link URL}
     *
     * @param configFileURL
     * @return ConfigurationSource for the input URL
     */
    public static ConfigurationSource getConfigurationSource(URL configFileURL) {
        return new URLConfigurationSource(configFileURL);
    }

    /**
     * Utility factory method for creating a {@link ConfigurationSource} based on InputStream
     *
     * @param configFileStream
     * @return ConfigurationSource for the input InputStream
     */
    public static ConfigurationSource getConfigurationSource(InputStream configFileStream) {
        return new InputStreamConfigurationSource(configFileStream);
    }

    /**
     * Utility factory method for creating a {@link ConfigurationSource} based on default settings (default ehcache.xml in classpath if one
     * is present or an ehcache-failsafe provided with the kit
     *
     * @return Default ConfigurationSource
     */
    public static ConfigurationSource getConfigurationSource() {
        return DefaultConfigurationSource.INSTANCE;
    }

    /**
     * Abstract method used for creating a {@link Configuration} based on the source
     *
     * @return {@link Configuration} based on the source
     */
    public abstract Configuration createConfiguration();

    /**
     * {@link ConfigurationSource} based on file name
     *
     */
    private static class FileNameSource extends ConfigurationSource {

        private final File file;

        /**
         * Constructor accepting the file name
         *
         * @param fileName
         */
        public FileNameSource(File file) {
            this.file = file;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.config.generator.ConfigurationSource#getConfiguration()
         */
        @Override
        public Configuration createConfiguration() {
            return ConfigurationFactory.parseConfiguration(file);
        }

        @Override
        public String toString() {
            return "FileNameSource [file=" + file + "]";
        }
    }

    /**
     * {@link ConfigurationSource} based on URL
     *
     */
    private static class URLConfigurationSource extends ConfigurationSource {
        private final URL url;

        /**
         * Constructor accepting a URL
         *
         * @param url
         */
        public URLConfigurationSource(URL url) {
            this.url = url;
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.config.generator.ConfigurationSource#createConfiguration()
         */
        @Override
        public Configuration createConfiguration() {
            return ConfigurationFactory.parseConfiguration(url);
        }

        @Override
        public String toString() {
            return "URLConfigurationSource [url=" + url + "]";
        }
    }

    /**
     * {@link ConfigurationSource} based on {@link InputStream}
     *
     */
    private static class InputStreamConfigurationSource extends ConfigurationSource {
        private final InputStream stream;

        /**
         * Constructor accepting {@link InputStream}
         *
         * @param stream
         */
        public InputStreamConfigurationSource(InputStream stream) {
            this.stream = stream;
            stream.mark(Integer.MAX_VALUE);
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.config.generator.ConfigurationSource#createConfiguration()
         */
        @Override
        public Configuration createConfiguration() {
            try {
                stream.reset();
                return ConfigurationFactory.parseConfiguration(stream);
            } catch (IOException e) {
                throw new CacheException(e);
            }
        }

        @Override
        public String toString() {
            return "InputStreamConfigurationSource [stream=" + stream + "]";
        }

    }

    /**
     * Default {@link ConfigurationSource} based on default ehcache.xml in classpath (if one is present) or the ehcache-failsafe.xml
     * provided with the kit
     *
     */
    private static class DefaultConfigurationSource extends ConfigurationSource {

        /**
         * Singleton instance of {@link DefaultConfigurationSource}
         */
        public static final DefaultConfigurationSource INSTANCE = new DefaultConfigurationSource();

        /**
         * private constructor
         */
        public DefaultConfigurationSource() {
            //
        }

        /**
         * {@inheritDoc}
         *
         * @see net.sf.ehcache.config.generator.ConfigurationSource#createConfiguration()
         */
        @Override
        public Configuration createConfiguration() {
            return ConfigurationFactory.parseConfiguration();
        }

        @Override
        public String toString() {
            return "DefaultConfigurationSource [ ehcache.xml or ehcache-failsafe.xml ]";
        }

    }

}
