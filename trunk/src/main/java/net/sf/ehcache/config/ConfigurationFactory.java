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
            throw new CacheException("Error configuring from " + file + ". Error was " + e.getMessage());
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
            throw new CacheException("Error configuring from " + url + ". Error was " + e.getMessage());
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
            throw new CacheException("Error configuring from input stream. Error was " + e.getMessage());
        }
        return configuration;
    }



}
