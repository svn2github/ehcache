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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.util.ClassLoaderUtil;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class which configures beans from XML, using reflection.
 *
 * @author Greg Luck
 * @version $Id$
 */
public final class ConfigurationFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationFactory.class.getName());

    private static final String DEFAULT_CLASSPATH_CONFIGURATION_FILE = "/ehcache.xml";
    private static final String FAILSAFE_CLASSPATH_CONFIGURATION_FILE = "/ehcache-failsafe.xml";

    /**
     * Constructor.
     */
    private ConfigurationFactory() {

    }

    /**
     * Configures a bean from an XML file.
     */
    public static Configuration parseConfiguration(final File file) throws CacheException {
        if (file == null) {
            throw new CacheException("Attempt to configure ehcache from null file.");
        }

        LOG.debug("Configuring ehcache from file: {}", file);
        Configuration configuration = null;
        InputStream input = null;
        try {
            input = new BufferedInputStream(new FileInputStream(file));
            configuration = parseConfiguration(input);
        } catch (Exception e) {
            throw new CacheException("Error configuring from " + file + ". Initial cause was " + e.getMessage(), e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                LOG.error("IOException while closing configuration input stream. Error was " + e.getMessage());
            }
        }
        return configuration;
    }

    /**
     * Configures a bean from an XML file available as an URL.
     */
    public static Configuration parseConfiguration(final URL url) throws CacheException {
        LOG.debug("Configuring ehcache from URL: {}", url);
        Configuration configuration;
        InputStream input = null;
        try {
            input = url.openStream();
            configuration = parseConfiguration(input);
        } catch (Exception e) {
            throw new CacheException("Error configuring from " + url + ". Initial cause was " + e.getMessage(), e);
        } finally {
            try {
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                LOG.error("IOException while closing configuration input stream. Error was " + e.getMessage());
            }
        }
        return configuration;
    }

    /**
     * Configures a bean from an XML file in the classpath.
     */
    public static Configuration parseConfiguration() throws CacheException {
        ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
        URL url = null;
        if (standardClassloader != null) {
            url = standardClassloader.getResource(DEFAULT_CLASSPATH_CONFIGURATION_FILE);
        }
        if (url == null) {
            url = ConfigurationFactory.class.getResource(DEFAULT_CLASSPATH_CONFIGURATION_FILE);
        }
        if (url != null) {
            LOG.debug("Configuring ehcache from ehcache.xml found in the classpath: " + url);
        } else {
            url = ConfigurationFactory.class.getResource(FAILSAFE_CLASSPATH_CONFIGURATION_FILE);

            LOG.warn("No configuration found. Configuring ehcache from ehcache-failsafe.xml "
                    + " found in the classpath: {}", url);

        }
        return parseConfiguration(url);
    }

    /**
     * Configures a bean from an XML input stream.
     */
    public static Configuration parseConfiguration(final InputStream inputStream) throws CacheException {

        LOG.debug("Configuring ehcache from InputStream");

        Configuration configuration = new Configuration();
        try {
            InputStream translatedInputStream = translateSystemProperties(inputStream);
            final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
            final BeanHandler handler = new BeanHandler(configuration);
            parser.parse(translatedInputStream, handler);
        } catch (Exception e) {
            throw new CacheException("Error configuring from input stream. Initial cause was " + e.getMessage(), e);
        }
        return configuration;
    }

    /**
     * Translates system properties which can be added as tokens to the config file using ${token} syntax.
     * <p/>
     * So, if the config file contains a character sequence "multicastGroupAddress=${multicastAddress}", and there is a system property
     * multicastAddress=230.0.0.12 then the translated sequence becomes "multicastGroupAddress=230.0.0.12"
     *
     * @param inputStream
     * @return a translated stream
     */
    private static InputStream translateSystemProperties(InputStream inputStream) throws IOException {

        StringBuffer stringBuffer = new StringBuffer();
        int c;
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        while ((c = reader.read()) != -1) {
            stringBuffer.append((char) c);
        }
        String configuration = stringBuffer.toString();

        Set tokens = extractPropertyTokens(configuration);
        for (Object tokenObject : tokens) {
            String token = (String) tokenObject;
            String leftTrimmed = token.replaceAll("\\$\\{", "");
            String trimmedToken = leftTrimmed.replaceAll("\\}", "");

            String property = System.getProperty(trimmedToken);
            if (property == null) {
                LOG.debug("Did not find a system property for the " + token +
                        " token specified in the configuration.Replacing with \"\"");
            } else {
                //replaceAll by default clobbers \ and $
                String propertyWithQuotesProtected = Matcher.quoteReplacement(property);
                configuration = configuration.replaceAll("\\$\\{" + trimmedToken + "\\}", propertyWithQuotesProtected);

                LOG.debug("Found system property value of " + property + " for the " + token +
                        " token specified in the configuration.");
            }
        }
        return new ByteArrayInputStream(configuration.getBytes());
    }

    /**
     * Extracts properties of the form ${...}
     *
     * @param sourceDocument the source document
     * @return a Set of properties. So, duplicates are only counted once.
     */
    static Set extractPropertyTokens(String sourceDocument) {
        Set propertyTokens = new HashSet();
        Pattern pattern = Pattern.compile("\\$\\{.+?\\}");
        Matcher matcher = pattern.matcher(sourceDocument);
        while (matcher.find()) {
            String token = matcher.group();
            propertyTokens.add(token);
        }
        return propertyTokens;
    }


}
