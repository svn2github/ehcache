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

import java.io.PrintWriter;
import java.io.StringWriter;

import net.sf.ehcache.CacheManager;
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
     * Generates Configuration text from a {@link CacheManager}
     *
     * @param cacheManager
     *            the cacheManager
     * @return text representing the cacheManager {@link Configuration}
     */
    public static String generateCacheManagerConfigurationText(CacheManager cacheManager) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        try {
            XMLGeneratorVisitor configGenerator = new XMLGeneratorVisitor(writer);
            configGenerator.disableOutputBehavior(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES);
            visitConfiguration(cacheManager, configGenerator);
            writer.flush();
        } finally {
            writer.close();
        }
        return output.toString();
    }

    /**
     * package protected access so that tests can have access
     *
     * @param cacheManager
     * @param visitor
     */
    static void visitConfiguration(CacheManager cacheManager, NodeElementVisitor visitor) {
        ConfigurationElement configElement = new ConfigurationElement(cacheManager);
        configElement.accept(visitor);
    }

    /**
     * Generates Configuration text from a {@link Configuration}
     *
     * @param configuration
     *            the configuration
     * @return text representing the {@link Configuration}
     */
    public static String generateCacheManagerConfigurationText(Configuration configuration) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        try {
            XMLGeneratorVisitor configGenerator = new XMLGeneratorVisitor(writer);
            configGenerator.disableOutputBehavior(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES);
            visitConfiguration(configuration, configGenerator);
            writer.flush();
        } finally {
            writer.close();
        }
        return output.toString();
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
    public static String generateCacheConfigurationText(Configuration configuration, CacheConfiguration cacheConfiguration) {
        StringWriter output = new StringWriter();
        PrintWriter writer = new PrintWriter(output);
        try {
            XMLGeneratorVisitor configGenerator = new XMLGeneratorVisitor(writer);
            configGenerator.disableOutputBehavior(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES);
            visitCacheConfiguration(configuration, cacheConfiguration, configGenerator);
            writer.flush();
        } finally {
            writer.close();
        }
        return output.toString();
    }

    /**
     * package protected access so that tests can have access
     *
     * @param cacheConfiguration
     * @param configGenerator
     */
    static void visitCacheConfiguration(Configuration configuration, CacheConfiguration cacheConfiguration, NodeElementVisitor configGenerator) {
        CacheConfigurationElement element = new CacheConfigurationElement(null, configuration, cacheConfiguration);
        element.accept(configGenerator);
    }

}
