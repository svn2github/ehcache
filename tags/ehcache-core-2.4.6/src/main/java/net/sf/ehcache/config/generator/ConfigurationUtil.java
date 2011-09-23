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

package net.sf.ehcache.config.generator;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.generator.model.NodeElementVisitor;
import net.sf.ehcache.config.generator.model.XMLGeneratorVisitor;
import net.sf.ehcache.config.generator.model.XMLGeneratorVisitor.OutputBehavior;
import net.sf.ehcache.config.generator.model.elements.CacheConfigurationElement;
import net.sf.ehcache.config.generator.model.elements.ConfigurationElement;

/**
 * Utility class with static methods for generating configuration texts in different ways based on input
 * 
 * <p />
 * 
 * @author <a href="mailto:asanoujam@terracottatech.com">Abhishek Sanoujam</a>
 * 
 */
public abstract class ConfigurationUtil {

    /**
     * Generates Configuration text from a {@link Configuration}
     * 
     * @param configuration
     *            the configuration
     * @return text representing the {@link Configuration}
     */
    public static String generateCacheManagerConfigurationText(Configuration configuration) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        XMLGeneratorVisitor configGenerator = new XMLGeneratorVisitor(out);
        configGenerator.disableOutputBehavior(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES);
        visitConfiguration(configuration, configGenerator);
        out.flush();
        out.close();
        return baos.toString();
    }

    /**
     * package protected access so that tests can have access
     * 
     * @param configuration
     * @param visitor
     */
    static void visitConfiguration(Configuration configuration, NodeElementVisitor visitor) {
        ConfigurationElement configElement = new ConfigurationElement(configuration);
        configElement.accept(visitor);
    }

    /**
     * Generates configuration text for a {@link CacheConfiguration}
     * 
     * @param cacheConfiguration
     *            the {@link CacheConfiguration}
     * @return text representing the {@link CacheConfiguration}
     */
    public static String generateCacheConfigurationText(CacheConfiguration cacheConfiguration) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter out = new PrintWriter(baos);
        XMLGeneratorVisitor configGenerator = new XMLGeneratorVisitor(out);
        configGenerator.disableOutputBehavior(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES);
        visitCacheConfiguration(cacheConfiguration, configGenerator);
        out.flush();
        out.close();
        return baos.toString();
    }

    /**
     * package protected access so that tests can have access
     * 
     * @param cacheConfiguration
     * @param configGenerator
     */
    static void visitCacheConfiguration(CacheConfiguration cacheConfiguration, NodeElementVisitor configGenerator) {
        CacheConfigurationElement element = new CacheConfigurationElement(null, cacheConfiguration);
        element.accept(configGenerator);
    }

}
