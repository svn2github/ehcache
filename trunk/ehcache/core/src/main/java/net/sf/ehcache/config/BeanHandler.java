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

package net.sf.ehcache.config;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A SAX handler that configures a bean.
 *
 * @version $Id$
 * @author Adam Murdoch
 * @author Greg Luck
 */
final class BeanHandler extends DefaultHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BeanHandler.class.getName());
    private final Object bean;
    private ElementInfo element;
    private Locator locator;

    // State for extracting a subtree
    private String subtreeMatchingQname;
    private StringBuilder subtreeText;
    private Method subtreeMethod;

    /**
     * Constructor.
     */
    public BeanHandler(final Object bean) {
        this.bean = bean;
    }

    /**
     * Receive a Locator object for document events.
     */
    @Override
    public final void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    private String getTagPart(String qName) {
        String[] parts = qName.split(":");
        return parts[parts.length - 1];
    }

    /**
     * Receive notification of the start of an element.
     */
    @Override
    public final void startElement(final String uri,
                             final String localName,
                             final String qName,
                             final Attributes attributes)
            throws SAXException {

        boolean subtreeAppend = extractingSubtree();
        if (extractingSubtree() || startExtractingSubtree(getTagPart(qName))) {
            if (subtreeAppend) {
                appendToSubtree("<" + qName);
            }

            for (int i = 0; i < attributes.getLength(); i++) {
                final String attrName = attributes.getQName(i);
                final String attrValue = attributes.getValue(i);
                if (subtreeAppend) {
                    appendToSubtree(" " + attrName + "=\"" + attrValue + "\"");
                }
            }

            if (subtreeAppend) {
                appendToSubtree(">");
            }
            element = new ElementInfo(element, qName, bean);
        } else {
            if (element == null) {
                element = new ElementInfo(qName, bean);
            } else {
                final Object child = createChild(element, qName);
                element = new ElementInfo(element, qName, child);
            }

            // Set the attributes
            for (int i = 0; i < attributes.getLength(); i++) {
                final String attrName = attributes.getQName(i);
                final String attrValue = attributes.getValue(i);
                setAttribute(element, attrName, attrValue);
            }
        }
    }

    /**
     * Receive notification of the end of an element.
     */
    @Override
    public final void endElement(final String uri,
                           final String localName,
                           final String qName)
            throws SAXException {

        if (element.parent != null) {
            if (extractingSubtree()) {
                if (endsSubtree(getTagPart(qName))) {
                    endSubtree();
                } else {
                    appendToSubtree("</" + qName + ">");
                }
            } else {
                addChild(element.parent.bean, element.bean, qName);
            }
        }
        element = element.parent;
    }

    /**
     * Receive notification of character data within an element - only used currently when
     * extracting an xml subtree
     */
    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {

        if (extractingSubtree()) {
            appendToSubtree(ch, start, length);
        }
    }

    /**
     * Creates a child element of an object.
     */
    private Object createChild(final ElementInfo parent, final String name)
            throws SAXException {

        try {
            // Look for a create<name> method
            final Class parentClass = parent.bean.getClass();
            Method method = findCreateMethod(parentClass, name);
            if (method != null) {
                return method.invoke(parent.bean, new Object[] {});
            }

            // Look for an add<name> method
            method = findSetMethod(parentClass, "add", name);
            if (method != null) {
                return createInstance(parent.bean, method.getParameterTypes()[0]);
            }
        } catch (final Exception e) {
            throw new SAXException(getLocation() + ": Could not create nested element <" + name + ">.", e);
        }

        throw new SAXException(getLocation()
                + ": Element <"
                + parent.elementName
                + "> does not allow nested <"
                + name
                + "> elements.");
    }

    /**
     * Creates a child object.
     */
    private static Object createInstance(Object parent, Class childClass)
            throws Exception {
        final Constructor[] constructors = childClass.getDeclaredConstructors();
        ArrayList candidates = new ArrayList();
        for (final Constructor constructor : constructors) {
            final Class[] params = constructor.getParameterTypes();
            if (params.length == 0) {
                candidates.add(constructor);
            } else if (params.length == 1 && params[0].isInstance(parent)) {
                candidates.add(constructor);
            }
        }
        switch (candidates.size()) {
            case 0:
                throw new Exception("No constructor for class " + childClass.getName());
            case 1:
                break;
            default:
                throw new Exception("Multiple constructors for class " + childClass.getName());
        }

        final Constructor constructor = (Constructor) candidates.remove(0);
        constructor.setAccessible(true);
        if (constructor.getParameterTypes().length == 0) {
            return constructor.newInstance(new Object[] {});
        } else {
            return constructor.newInstance(new Object[]{parent});
        }
    }

    /**
     * Finds a creator method.
     */
    private static Method findCreateMethod(Class objClass, String name) {
        final String methodName = makeMethodName("create", name);
        final Method[] methods = objClass.getMethods();
        for (final Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterTypes().length != 0) {
                continue;
            }
            if (method.getReturnType().isPrimitive() || method.getReturnType().isArray()) {
                continue;
            }
            return method;
        }

        return null;
    }

    /**
     * Builds a method name from an element or attribute name.
     */
    private static String makeMethodName(final String prefix, final String name) {
        String rawName = prefix + Character.toUpperCase(name.charAt(0)) + name.substring(1);

        // Remove "-" in element name
        return rawName.replace("-", "");
    }

    /**
     * Sets an attribute.
     */
    private void setAttribute(final ElementInfo element,
                              final String attrName,
                              final String attrValue)
            throws SAXException {
        try {
            // Look for a set<name> method
            final Class objClass = element.bean.getClass();
            final Method method = chooseSetMethod(objClass, "set", attrName, String.class);
            if (method != null) {
                final Object realValue = convert(attrName, method.getParameterTypes()[0], attrValue);
                method.invoke(element.bean, new Object[]{realValue});
                return;
            } else {
                //allow references to an XML schema but do not use it
                if (element.elementName.equals("ehcache")) {
                    LOG.debug("Ignoring ehcache attribute {}", attrName);
                    return;
                }
            }
        } catch (final InvocationTargetException e) {
            throw new SAXException(getLocation() + ": Could not set attribute \"" + attrName + "\"."
                + ". Message was: " + e.getTargetException());
        } catch (final Exception e) {
            throw new SAXException(getLocation() + ": Could not set attribute \"" + attrName + "\" - " + e.getMessage());
        }

        throw new SAXException(getLocation()
                + ": Element <"
                + element.elementName
                + "> does not allow attribute \""
                + attrName
                + "\".");
    }

    /**
     * Converts a string to an object of a particular class.
     * @param attrName Name of attribute
     */
    private static Object convert(String attributeName, final Class toClass, final String value)
            throws Exception {
        if (value == null) {
            return null;
        }
        if (toClass.isInstance(value)) {
            return value;
        }
        if (toClass == Long.class || toClass == Long.TYPE) {
            return Long.decode(value);
        }
        if (toClass == Integer.class || toClass == Integer.TYPE) {
            return Integer.decode(value);
        }
        if (toClass == Boolean.class || toClass == Boolean.TYPE) {
            if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
                return Boolean.valueOf(value);
            } else {
                throw new InvalidConfigurationException("Invalid value specified for attribute '" + attributeName
                        + "', please use 'true' or 'false' instead of '" + value + "'");
            }
        }
        throw new Exception("Cannot convert attribute value to class " + toClass.getName());
    }

    private Method chooseSetMethod(final Class objClass, final String prefix, final String name, final Class preferredParameterType)
            throws Exception {
        final String methodName = makeMethodName(prefix, name);
        final Method[] methods = objClass.getMethods();
        Set<Method> candidates = new HashSet<Method>();
        for (final Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getReturnType().equals(Void.TYPE)) {
                continue;
            }
            candidates.add(method);
        }
        if (candidates.size() == 0) {
            return null;
        } else if (candidates.size() == 1) {
            return candidates.iterator().next();
        } else {
            for (Method m : candidates) {
                if (m.getParameterTypes()[0].equals(preferredParameterType)) {
                    return m;
                }
            }
            throw new Exception("Multiple " + methodName + "() methods found in class " + objClass.getName()
                    + ", but not one with preferred parameter type - " + preferredParameterType.getName());
        }
    }

    /**
     * Finds a setter method.
     */
    private Method findSetMethod(final Class objClass,
                                 final String prefix,
                                 final String name)
            throws Exception {
        final String methodName = makeMethodName(prefix, name);
        final Method[] methods = objClass.getMethods();
        Method candidate = null;
        for (final Method method : methods) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            if (Modifier.isStatic(method.getModifiers())) {
                continue;
            }
            if (method.getParameterTypes().length != 1) {
                continue;
            }
            if (!method.getReturnType().equals(Void.TYPE)) {
                continue;
            }
            if (candidate != null) {
                throw new Exception("Multiple " + methodName + "() methods in class " + objClass.getName() + ".");
            }
            candidate = method;
        }

        return candidate;
    }

    /**
     * Attaches a child element to its parent.
     */
    private void addChild(final Object parent,
                          final Object child,
                          final String name)
            throws SAXException {
        try {
            // Look for an add<name> method on the parent
            final Method method = findSetMethod(parent.getClass(), "add", name);
            if (method != null) {
                method.invoke(parent, new Object[]{child});
            }
        } catch (final InvocationTargetException e) {
            final SAXException exc = new SAXException(getLocation() + ": Could not finish element <" + name + ">." +
                    " Message was: " + e.getTargetException());
            throw exc;
        } catch (final Exception e) {
            throw new SAXException(getLocation() + ": Could not finish element <" + name + ">.");
        }
    }

    /**
     * Formats the current document location.
     */
    private String getLocation() {
        return locator.getSystemId() + ':' + locator.getLineNumber();
    }

    /**
     * Determine whether we should start extracting a subtree, based on
     * whether there is an extract method for this tag in the parent bean.
     */
    private boolean startExtractingSubtree(String name) throws SAXException {
        // if need to start extracting, stow the name
        if (element == null || element.bean == null) {
            return false;
        }

        try {
            final Method method = findSetMethod(element.bean.getClass(), "extract", name);
            if (method != null) {
                subtreeMatchingQname = name;
                subtreeText = new StringBuilder();
                subtreeMethod = method;
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            throw new SAXException(getLocation() + ": Error checking for extract method on <" + name + ">.");
        }
    }

    private boolean extractingSubtree() {
        return this.subtreeMatchingQname != null;
    }

    /**
     * Append to the current extracted subtree text
     */
    private void appendToSubtree(String text) {
        subtreeText.append(text);
    }

    /**
     * Append to the current extracted subtree text
     */
    private void appendToSubtree(char[] text, int start, int length) {
        subtreeText.append(text, start, length);
    }

    /**
     * Determine whether the current endName tag ends the subtree matching
     */
    private boolean endsSubtree(String endName) {
        return this.subtreeMatchingQname != null && this.subtreeMatchingQname.equals(endName);
    }

    private void endSubtree() throws SAXException {
        try {
            subtreeMethod.invoke(element.parent.bean, new Object[]{subtreeText.toString()});
        } catch (InvocationTargetException e) {
            throw new SAXException(getLocation() + ": Could not set extracted subtree \"" + subtreeMatchingQname + "\"."
                + " Message was: " + e.getTargetException());
        } catch (Exception e) {
            throw new SAXException(getLocation() + ": Could not set extracted subtree \"" + subtreeMatchingQname + "\"."
                + " Message was: " + e.getMessage());
        }

        subtreeMatchingQname = null;
        subtreeMethod = null;
        subtreeText = null;
    }

    /**
     * Element info class
     */
    private static final class ElementInfo {
        private final ElementInfo parent;
        private final String elementName;
        private final Object bean;

        public ElementInfo(final String elementName, final Object bean) {
            parent = null;
            this.elementName = elementName;
            this.bean = bean;
        }

        public ElementInfo(final ElementInfo parent, final String elementName, final Object bean) {
            this.parent = parent;
            this.elementName = elementName;
            this.bean = bean;
        }
    }
}
