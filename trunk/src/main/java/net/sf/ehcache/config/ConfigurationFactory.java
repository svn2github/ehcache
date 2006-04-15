/**
 *  Copyright 2003-2006 Greg Luck
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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.util.ClassLoaderUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * A utility class which configures beans from XML, using reflection.
 *
 * @author Greg Luck
 * @version $Id: ConfigurationFactory.java,v 1.1 2006/03/09 06:38:19 gregluck Exp $
 */
public final class ConfigurationFactory {
    private static final Log LOG = LogFactory.getLog(ConfigurationFactory.class.getName());

    private static final String DEFAULT_CLASSPATH_CONFIGURATION_FILE = "/ehcache.xml";
    private static final String FAILSAFE_CLASSPATH_CONFIGURATION_FILE = "/ehcache-failsafe.xml";

    /**
     * Constructor
     */
    private ConfigurationFactory() {

    }

    /**
     * Configures a bean from an XML file.
     */
    public static Configuration parseConfiguration(final File file) throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Configuring ehcache from file: " + file.toString());
        }
        Configuration configuration = null;
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            configuration = parseConfiguration(input);
        } catch (Exception e) {
            throw new CacheException("Error configuring from " + file + ". Initial cause was " + e.getMessage(), e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                throw new CacheException("IOException while closing configuration input stream. Error was "
                        + e.getMessage());
            }
        }
        return configuration;
    }

    /**
     * Configures a bean from an XML file available as an URL.
     */
    public static Configuration parseConfiguration(final URL url) throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Configuring ehcache from URL: " + url);
        }
        Configuration configuration;
        InputStream input = null;
        try {
            input = url.openStream();
            configuration = parseConfiguration(input);
        } catch (Exception e) {
            throw new CacheException("Error configuring from " + url + ". Initial cause was " + e.getMessage(), e);
        } finally {
            try {
                input.close();
            } catch (IOException e) {
                throw new CacheException("IOException while closing configuration input stream. Error was "
                        + e.getMessage());
            }
        }
        return configuration;
    }

    /**
     * Configures a bean from an XML file in the classpath.
     */
    public static Configuration parseConfiguration() throws CacheException {
        ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
        Configuration configuration;
        URL url = null;
        if (standardClassloader != null) {
            url = standardClassloader.getResource(DEFAULT_CLASSPATH_CONFIGURATION_FILE);
        }
        if (url == null) {
            url = ConfigurationFactory.class.getResource(DEFAULT_CLASSPATH_CONFIGURATION_FILE);
        }
        if (url != null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Configuring ehcache from ehcache.xml found in the classpath: " + url);
            }
        } else {
            url = ConfigurationFactory.class.getResource(FAILSAFE_CLASSPATH_CONFIGURATION_FILE);
            if (LOG.isWarnEnabled()) {
                LOG.warn("No configuration found. Configuring ehcache from ehcache-failsafe.xml "
                        + " found in the classpath: " + url);
            }
        }
        return parseConfiguration(url);
    }

    /**
     * Configures a bean from an XML input stream
     */
    public static Configuration parseConfiguration(final InputStream inputStream) throws CacheException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Configuring ehcache from InputStream");
        }
        Configuration configuration = new Configuration();
        try {
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final BeanHandler handler = new BeanHandler(configuration);
            parser.parse(inputStream, handler);
        } catch (Exception e) {
            throw new CacheException("Error configuring from input stream. Initial cause was " + e.getMessage(), e);
        }
        return configuration;
    }

}
