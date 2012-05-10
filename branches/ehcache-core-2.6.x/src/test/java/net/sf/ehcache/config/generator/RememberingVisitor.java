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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import net.sf.ehcache.config.generator.model.AbstractDepthFirstVisitor;
import net.sf.ehcache.config.generator.model.NodeAttribute;
import net.sf.ehcache.config.generator.model.NodeElement;

public class RememberingVisitor extends AbstractDepthFirstVisitor {
    private final Set<NodeElement> visitedElements = new HashSet<NodeElement>();
    private final ConcurrentMap<NodeElement, Set<NodeAttribute>> visitedAttributes = new ConcurrentHashMap<NodeElement, Set<NodeAttribute>>();

    public Set<NodeElement> getVisitedElements() {
        return visitedElements;
    }

    public ConcurrentMap<NodeElement, Set<NodeAttribute>> getVisitedAttributes() {
        return visitedAttributes;
    }

    @Override
    protected void visitElement(NodeElement element) {
        visitedElements.add(element);
    }

    @Override
    protected void visitAttributes(NodeElement element, List<NodeAttribute> attributes) {
        getVisitedAttributesForElement(element).addAll(attributes);
    }

    private Set<NodeAttribute> getVisitedAttributesForElement(NodeElement element) {
        Set<NodeAttribute> set = visitedAttributes.get(element);
        if (set == null) {
            set = new HashSet<NodeAttribute>();
            Set<NodeAttribute> prev = visitedAttributes.putIfAbsent(element, set);
            if (prev != null) {
                set = prev;
            }
        }
        return set;
    }
}