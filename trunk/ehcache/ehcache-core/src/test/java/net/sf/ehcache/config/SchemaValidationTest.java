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

package net.sf.ehcache.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.terracotta.test.categories.CheckShorts;
import org.w3c.dom.Document;

/**
 * Tests that the Ehcache schema passes validation
 *
 * @author Alex Miller
 */
@Category(CheckShorts.class)
public class SchemaValidationTest {

    private static final String SRC_CONFIG_DIR = "src/main/config/";

    /**
     * Test that schema validates
     */
    @Test
    public void testSchemaValidates() throws Exception {
        InputStream docStream = new FileInputStream(new File(SRC_CONFIG_DIR + "ehcache.xml").getAbsolutePath());
        InputStream xsdStream = new FileInputStream(new File(SRC_CONFIG_DIR + "ehcache.xsd").getAbsolutePath());

        try {
            // parse an XML document into a DOM tree
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            docFactory.setNamespaceAware(true);
            DocumentBuilder parser = docFactory.newDocumentBuilder();
            Document document = parser.parse(docStream);

            // create a SchemaFactory capable of understanding the schemas
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

            // load the schema, represented by a Schema instance
            Source schemaFile = new StreamSource(xsdStream);
            Schema schema = factory.newSchema(schemaFile);

            // create a Validator instance, which can be used to validate an instance document
            Validator validator = schema.newValidator();

            // validate the DOM tree
            validator.validate(new DOMSource(document));

        } finally {
            docStream.close();
            xsdStream.close();
        }
    }
}
