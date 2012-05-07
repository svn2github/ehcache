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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.ehcache.config.generator.model.NodeAttribute;
import net.sf.ehcache.config.generator.model.NodeElement;
import net.sf.ehcache.config.generator.model.XMLGeneratorVisitor;

public class ElementIgnoringXMLGenerator extends XMLGeneratorVisitor {

    private final Set<String> ignoredElements;

    public ElementIgnoringXMLGenerator(PrintWriter out, String... ignoredElements) {
        super(out);
        this.ignoredElements = new HashSet<String>(Arrays.asList(ignoredElements));
    }

    @Override
    protected void endAttributes(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.endAttributes(element);
    }

    @Override
    protected void endChildren(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.endChildren(element);
    }

    @Override
    protected void endElement(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.endElement(element);
    }

    @Override
    protected void startAttributes(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.startAttributes(element);
    }

    @Override
    protected void startChildren(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.startChildren(element);
    }

    @Override
    protected void startElement(NodeElement element) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.startElement(element);
    }

    @Override
    protected void visitAttributes(NodeElement element, List<NodeAttribute> attributes) {
        if (ignoredElements.contains(element.getName())) {
            return;
        }
        super.visitAttributes(element, attributes);
    }

}
