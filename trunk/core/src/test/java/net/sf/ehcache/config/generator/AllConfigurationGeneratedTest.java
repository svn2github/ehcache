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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import junit.framework.TestCase;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.generator.model.NodeAttribute;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.XMLGeneratorVisitor;
import net.sf.ehcache.config.generator.xsom.XSOMHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AllConfigurationGeneratedTest extends TestCase {

    private static final Logger LOG = LoggerFactory.getLogger(AllConfigurationGeneratedTest.class);

    private static final String SRC_CONFIG_DIR = "src/main/config/";
    private static final String TEST_RESOURCES_DIR = "src/test/resources/";
    private static int counter;
    private final int spacer = 0;

    public static void main(String[] args) throws Exception {
        new AllConfigurationGeneratedTest().testConfigurationGenerated();
    }

    public void testConfigurationGenerated() throws Exception {
        doTest(generateStandaloneEntryBasedConfigAttributesValueFactory());
        doTest(generateClusteredEntryBasedConfigAttributesValueFactory());
        doTest(generateStandaloneSizeBasedConfigAttributesValueFactory());
        doTest(generateClusteredSizeBasedConfigAttributesValueFactory());
    }

    private NodeElement generateStandaloneEntryBasedConfigAttributesValueFactory() throws Exception {
        File ehcacheXsd = new File(SRC_CONFIG_DIR + "ehcache.xsd");
        NodeElement ehcacheCompleteXsdElement = new XSOMHelper(new StandaloneEntryBasedConfigAttributesValueFactory()).createRootElement(
                new FileInputStream(ehcacheXsd), "ehcache");
        return ehcacheCompleteXsdElement;
    }

    private NodeElement generateClusteredEntryBasedConfigAttributesValueFactory() throws Exception {
        File ehcacheXsd = new File(SRC_CONFIG_DIR + "ehcache.xsd");
        NodeElement ehcacheCompleteXsdElement = new XSOMHelper(new ClusteredEntryBasedConfigAttributesValueFactory()).createRootElement(
                new FileInputStream(ehcacheXsd), "ehcache");
        return ehcacheCompleteXsdElement;
    }

    private NodeElement generateStandaloneSizeBasedConfigAttributesValueFactory() throws Exception {
        File ehcacheXsd = new File(SRC_CONFIG_DIR + "ehcache.xsd");
        NodeElement ehcacheCompleteXsdElement = new XSOMHelper(new StandaloneSizeBasedConfigAttributesValueFactory()).createRootElement(
                new FileInputStream(ehcacheXsd), "ehcache");
        return ehcacheCompleteXsdElement;
    }

    private NodeElement generateClusteredSizeBasedConfigAttributesValueFactory() throws Exception {
        File ehcacheXsd = new File(SRC_CONFIG_DIR + "ehcache.xsd");
        NodeElement ehcacheCompleteXsdElement = new XSOMHelper(new ClusteredSizeBasedConfigAttributesValueFactory()).createRootElement(
                new FileInputStream(ehcacheXsd), "ehcache");
        return ehcacheCompleteXsdElement;
    }

    private void doTest(NodeElement ehcacheCompleteXsdElement) throws IOException {
        String ehcacheXmlWithoutEmbeddedConfig = writeEhcacheXmlWithoutEmbeddedConfig(ehcacheCompleteXsdElement);
        UnvisitedHolder unvisited = getUnvisited(ehcacheCompleteXsdElement, ehcacheXmlWithoutEmbeddedConfig);
        for (NodeElement unvisitedElement : unvisited.unvisitedElements) {
            // its known "tc-config" will not be thr as its not included in the generated config
            if ("tc-config".equals(unvisitedElement.getName())) {
                unvisited.unvisitedElements.remove(unvisitedElement);
                break;
            }
        }
        checkUnvisited(unvisited);

        String ehcacheXmlWithoutTCUrl = writeEhcacheXmlWithoutTcURL(ehcacheCompleteXsdElement);
        unvisited = getUnvisited(ehcacheCompleteXsdElement, ehcacheXmlWithoutTCUrl);
        for (NodeElement element : unvisited.unvisitedAttributes.keySet()) {
            String eltName = element.getName();
            // it is known attribute "url" of "terracottaConfig" element will not be thr
            if ("terracottaConfig".equals(eltName)) {
                Set<NodeAttribute> attributes = unvisited.unvisitedAttributes.get(element);
                for (NodeAttribute attribute : attributes) {
                    if ("url".equals(attribute.getName())) {
                        attributes.remove(attribute);
                        break;
                    }
                }
            }
        }
        checkUnvisited(unvisited);
    }

    private void checkUnvisited(UnvisitedHolder unvisited) {
        String msg = "Some elements and/or attributes are not generated by the config generator. List of ungenerated elements/attribtues: \n";

        boolean fail = false;
        if (!unvisited.unvisitedElements.isEmpty()) {
            fail = true;
            msg += "Unvisited Elements: ";
            for (NodeElement element : unvisited.unvisitedElements) {
                msg += "\n   " + element.getFQName();
            }
            msg += "\n";
        }
        boolean first = true;
        for (NodeElement element : unvisited.unvisitedAttributes.keySet()) {
            Set<NodeAttribute> attributes = unvisited.unvisitedAttributes.get(element);
            if (!attributes.isEmpty()) {
                fail = true;
                if (first) {
                    msg += "Unvisited Attributes: ";
                    first = false;
                }
                for (NodeAttribute attribute : attributes) {
                    msg += "\n   Attribute '" + attribute.getName() + "' of element '" + element.getFQName() + "'";
                }
            }
        }
        if (fail) {
            LOG.info(msg);
            throw new AssertionError(msg);
        }
    }

    private UnvisitedHolder getUnvisited(NodeElement ehcacheCompleteElement, String ehcacheXml) throws IOException {
        UnvisitedHolder rv = new UnvisitedHolder();
        RememberingVisitor completeXsdVisitor = new RememberingVisitor();
        ehcacheCompleteElement.accept(completeXsdVisitor);

        ConfigurationSource configurationSource = ConfigurationSource.getConfigurationSource(new File(ehcacheXml));
        Configuration configuration = configurationSource.createConfiguration();
        RememberingVisitor generatedConfigVisitor = new RememberingVisitor();
        // use code in ConfigurationUtil
        ConfigurationUtil.visitConfiguration(configuration, generatedConfigVisitor);

        rv.unvisitedElements = new HashSet<NodeElement>(completeXsdVisitor.getVisitedElements());
        rv.unvisitedElements.removeAll(generatedConfigVisitor.getVisitedElements());

        rv.unvisitedAttributes = new HashMap<NodeElement, Set<NodeAttribute>>();
        for (NodeElement element : completeXsdVisitor.getVisitedAttributes().keySet()) {
            Set<NodeAttribute> attributes = new HashSet<NodeAttribute>(completeXsdVisitor.getVisitedAttributes().get(element));
            Set<NodeAttribute> generatedConfigVisitedAttrs = generatedConfigVisitor.getVisitedAttributes().get(element);
            if (generatedConfigVisitedAttrs != null) {
                attributes.removeAll(generatedConfigVisitedAttrs);
            }
            rv.unvisitedAttributes.put(element, attributes);
        }

        return rv;
    }

    private String writeEhcacheXmlWithoutEmbeddedConfig(NodeElement ehcacheCompleteElement) throws IOException {
        File tmpFile = File.createTempFile("ehcache-dummy", ".xml");
        LOG.info("ehcache-dummy file: " + tmpFile.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(tmpFile);
        PrintWriter pw = new PrintWriter(out);
        XMLGeneratorVisitor visitor = new ElementIgnoringXMLGenerator(pw, "tc-config");
        ehcacheCompleteElement.accept(visitor);
        pw.close();
        return tmpFile.getAbsolutePath();
    }

    private String writeEhcacheXmlWithoutTcURL(NodeElement ehcacheCompleteElement) throws IOException {
        File tmpFile = File.createTempFile("ehcache-dummy", ".xml");
        LOG.info("ehcache-dummy file: " + tmpFile.getAbsolutePath());
        FileOutputStream out = new FileOutputStream(tmpFile);
        PrintWriter pw = new PrintWriter(out);
        String[] ignored;
        XMLGeneratorVisitor visitor = new AttributeIgnoringXMLGenerator(pw, "url");
        ehcacheCompleteElement.accept(visitor);
        pw.close();
        return tmpFile.getAbsolutePath();
    }

    private static class UnvisitedHolder {
        private Map<NodeElement, Set<NodeAttribute>> unvisitedAttributes;
        private Set<NodeElement> unvisitedElements;
    }

}
