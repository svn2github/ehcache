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

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * Implementation of an ElementVisitor extending from {@link AbstractDepthFirstVisitor} which can generate XML out of a {@link NodeElement}.
 * Accepts a {@link PrintWriter} in the constructor and uses it to output the generated XML.
 * Output can be controlled by enabling/disabling the various options present in {@link OutputBehavior} by calling
 * {@link #enableOutputBehavior(OutputBehavior)} or {@link #disableOutputBehavior(OutputBehavior)}
 *
 * @author Abhishek Sanoujam
 *
 */
public class XMLGeneratorVisitor extends AbstractDepthFirstVisitor {

    /**
     * Enum controlling the generated XML output
     *
     * @author Abhishek Sanoujam
     *
     */
    public static enum OutputBehavior {
        /**
         * Output behavior controlling whether child elements should be indented or not
         */
        INDENT_CHIlD_ELEMENTS,
        /**
         * Output behavior controlling whether new lines should be added for each child element
         */
        NEWLINE_FOR_EACH_ELEMENT,
        /**
         * Output behavior controlling whether new lines should be added for each attribute
         */
        NEWLINE_FOR_EACH_ATTRIBUTE,
        /**
         * Output behavior controlling whether optional attributes having default values should be generated or not
         */
        OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES,
        /**
         * Output behavior controlling whether new lines should be added at the end or not
         */
        NEWLINE_AT_END;
    }

    private static final String SPACER = "    ";

    private final Map<OutputBehavior, Boolean> enabledOutputBehaviors = new HashMap<OutputBehavior, Boolean>();
    private final PrintWriter out;
    private int indent;
    private NodeElement rootElement;
    private boolean visitedFirstElement;

    /**
     * Constructor accepting the {@link PrintWriter}. All output behaviors are enabled by default.
     *
     * @param out
     *            the {@link PrintWriter}
     */
    public XMLGeneratorVisitor(PrintWriter out) {
        this.out = out;
        enableAllOutputBehaviors();
    }

    /**
     * Enables all output behaviors
     */
    public void enableAllOutputBehaviors() {
        for (OutputBehavior behavior : OutputBehavior.values()) {
            enableOutputBehavior(behavior);
        }
    }

    /**
     * Disables all output behaviors
     */
    public void disableAllOutputBehaviors() {
        enabledOutputBehaviors.clear();
    }

    /**
     * Enables one particular {@link OutputBehavior}
     *
     * @param behavior
     */
    public void enableOutputBehavior(OutputBehavior behavior) {
        enabledOutputBehaviors.put(behavior, Boolean.TRUE);
    }

    /**
     * Disables one particular {@link OutputBehavior}
     *
     * @param behavior
     */
    public void disableOutputBehavior(OutputBehavior behavior) {
        enabledOutputBehaviors.remove(behavior);
    }

    /**
     * Returns true if the output behavior is enabled
     *
     * @param behavior
     *            the output behavior to inspect
     * @return true if enabled, otherwise false
     */
    public boolean isOutputBehaviorEnabled(OutputBehavior behavior) {
        Boolean enabled = enabledOutputBehaviors.get(behavior);
        return enabled != null && enabled;
    }

    private void print(String string) {
        out.print(spacer() + string);
    }

    private void printWithoutSpacer(String string) {
        out.print(string);
    }

    private void newLine() {
        out.println(spacer());
    }

    private String spacer() {
        StringBuilder sb = new StringBuilder(SPACER.length() * indent);
        for (int i = 0; i < indent; i++) {
            sb.append(SPACER);
        }
        return sb.toString();
    }

    private void indentForward() {
        indent++;
    }

    private void indentBackward() {
        indent--;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startElement(NodeElement element) {
        if (isOutputBehaviorEnabled(OutputBehavior.NEWLINE_FOR_EACH_ELEMENT) && visitedFirstElement) {
            newLine();
        }
        print("<" + element.getName());
        if (!visitedFirstElement) {
            rootElement = element;
            visitedFirstElement = true;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startAttributes(NodeElement element) {
        if (isOutputBehaviorEnabled(OutputBehavior.NEWLINE_FOR_EACH_ATTRIBUTE)) {
            indentForward();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void visitAttributes(NodeElement element, List<NodeAttribute> attributes) {
        for (NodeAttribute attribute : attributes) {
            visitAttribute(element, attribute);
        }
    }

    /**
     * Visits an attribute.
     *
     * @param element
     * @param attribute
     */
    protected void visitAttribute(NodeElement element, NodeAttribute attribute) {
        String value = attribute.getValue();
        if (!isOutputBehaviorEnabled(OutputBehavior.OUTPUT_OPTIONAL_ATTRIBUTES_WITH_DEFAULT_VALUES)) {
            if (attribute.isOptional()) {
                if (value != null && value.equals(attribute.getDefaultValue())) {
                    // do not optional attributes with default values, as defined by outputBehaviors
                    return;
                }
            }
        }
        if (value == null) {
            value = attribute.getDefaultValue();
        }
        if (value != null) {
            printWithoutSpacer(" ");
            String line = attribute.getName() + "=\"" + value + "\"";
            if (isOutputBehaviorEnabled(OutputBehavior.NEWLINE_FOR_EACH_ATTRIBUTE)) {
                newLine();
                print(line);
            } else {
                printWithoutSpacer(line);
            }
        }

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endAttributes(NodeElement element) {
        String end = (element.getInnerContent() == null && !element.hasChildren()) ? "/>" : ">";
        printWithoutSpacer(end);
        if (isOutputBehaviorEnabled(OutputBehavior.NEWLINE_FOR_EACH_ATTRIBUTE)) {
            indentBackward();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void visitElement(NodeElement element) {
        if (element.getInnerContent() != null) {
            indentForward();
            newLine();
            print(element.getInnerContent());
            indentBackward();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void startChildren(NodeElement element) {
        if (isOutputBehaviorEnabled(OutputBehavior.INDENT_CHIlD_ELEMENTS)) {
            indentForward();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endChildren(NodeElement element) {
        if (isOutputBehaviorEnabled(OutputBehavior.INDENT_CHIlD_ELEMENTS)) {
            indentBackward();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void endElement(NodeElement element) {
        if (element.getInnerContent() != null || element.hasChildren()) {
            if (isOutputBehaviorEnabled(OutputBehavior.NEWLINE_FOR_EACH_ELEMENT)) {
                newLine();
            }
            print("</" + element.getName() + ">");
        }
        if (element.equals(rootElement) && isOutputBehaviorEnabled(OutputBehavior.NEWLINE_AT_END)) {
            newLine();
        }
    }

}
