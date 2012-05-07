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

package net.sf.ehcache.config.generator.model;

import java.util.List;

/**
 * 
 * Implementation of {@link NodeElementVisitor} that does a depth-first traversal of the element. Depth first means the visit starts at the
 * element and goes deeper and deeper in each child element until there are no children and then backtracks, doing the same thing for each
 * children in each child element.
 * This class is an abstract class and provides empty methods that sub-classes can override as needed. The visit methods in this class are
 * called in this order for each element visited by this visitor:
 * <ul>
 * <li>{@link #startElement(NodeElement)}</li>
 * <li>{@link #startAttributes(NodeElement)}</li>
 * <li>{@link #visitAttributes(NodeElement, List)}</li>
 * <li>{@link #endAttributes(NodeElement)}</li>
 * <li>{@link #visitElement(NodeElement)}</li>
 * <li>{@link #startChildren(NodeElement)}</li>
 * <li>same sequence for each child element</li>
 * <li>{@link #endChildren(NodeElement)}</li>
 * <li>{@link #endElement(NodeElement)}</li>
 * </ul>
 * 
 * @author Abhishek Sanoujam
 * 
 */
public abstract class AbstractDepthFirstVisitor implements NodeElementVisitor {

    /**
     * {@inheritDoc}
     */
    public void visit(NodeElement element) {
        if (element == null) {
            throw new NullPointerException("element cannot be null");
        }
        doDfs(element);
    }

    private void doDfs(NodeElement element) {
        startElement(element);
        startAttributes(element);
        visitAttributes(element, element.getAttributes());
        endAttributes(element);
        visitElement(element);
        startChildren(element);
        for (NodeElement child : element.getChildElements()) {
            doDfs(child);
        }
        endChildren(element);
        endElement(element);
    }

    /**
     * Starts visiting an element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void startElement(NodeElement element) {
        // override if needed
    }

    /**
     * Starts visiting the attributes of the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void startAttributes(NodeElement element) {
        // override if needed
    }

    /**
     * Visits the attributes of the element. Override as needed
     * 
     * @param element
     *            the element
     * @param attributes
     *            the attributes
     */
    protected void visitAttributes(NodeElement element, List<NodeAttribute> attributes) {
        // override if needed
    }

    /**
     * Finish visiting attributes of the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void endAttributes(NodeElement element) {
        // override if needed
    }

    /**
     * Visits the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void visitElement(NodeElement element) {
        // override if needed
    }

    /**
     * Starts visiting children of the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void startChildren(NodeElement element) {
        // override if needed
    }

    /**
     * Finish visiting children of the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void endChildren(NodeElement element) {
        // override if needed
    }

    /**
     * Finish visiting the element. Override as needed
     * 
     * @param element
     *            the element
     */
    protected void endElement(NodeElement element) {
        // override if needed
    }

}
