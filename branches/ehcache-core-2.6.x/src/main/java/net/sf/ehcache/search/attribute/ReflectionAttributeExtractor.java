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

package net.sf.ehcache.search.attribute;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.sf.ehcache.Element;
import net.sf.ehcache.config.InvalidConfigurationException;

/**
 * Built-in search attribute extractor driven by method/value dotted expression
 * chains.<br>
 * <br>
 * The expression chain must start with one of either "key", "value", or
 * "element". From the starting object a chain of either method calls or field
 * names follows. Method calls and field names can be freely mixed in the chain.
 * Some examples:
 * <ol>
 * <li>"key.getName()" -- call getName() on the key object
 * <li>"value.person.getAge()" -- get the "person" field of the value object and call getAge() on it
 * <li>"element.toString()" -- call toString() on the element
 * </ol>
 * The method and field name portions of the expression are case sensitive
 *
 * @author teck
 */
public class ReflectionAttributeExtractor implements AttributeExtractor {

    private static final String ELEMENT = "element";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private final String expression;
    private final Part[] parts;
    private final StartType start;

    /**
     * Create a new ReflectionAttributeExtractor
     *
     * @param expression
     */
    public ReflectionAttributeExtractor(String expression) throws InvalidConfigurationException {
        String trimmed = expression.trim();

        String[] tokens = trimmed.split("\\.");

        if (tokens.length == 0) {
            throw new InvalidConfigurationException("Invalid attribute expression: " + trimmed);
        }

        String startToken = tokens[0];

        if (startToken.equalsIgnoreCase(ELEMENT)) {
            start = StartType.ELEMENT;
        } else if (startToken.equalsIgnoreCase(KEY)) {
            start = StartType.KEY;
        } else if (startToken.equalsIgnoreCase(VALUE)) {
            start = StartType.VALUE;
        } else {
            throw new InvalidConfigurationException("Expression must start with either \"" + ELEMENT + "\", \"" + KEY + "\" or \"" + VALUE
                    + "\": " + trimmed);
        }

        this.parts = parseExpression(tokens, trimmed);
        this.expression = trimmed;
    }

    /**
     * Evaluate the expression for the given element
     *
     * @return the attribute value
     * @throws AttributeExtractorException if there is an error in evaluating the expression
     */
    public Object attributeFor(Element e, String attributeName) throws AttributeExtractorException {
        // NOTE: We can play all kinds of tricks of generating java classes and
        // using Unsafe if needed

        Object startObject;
        switch (start) {
            case ELEMENT: {
                startObject = e;
                break;
            }
            case KEY: {
                startObject = e.getObjectKey();
                break;
            }
            case VALUE: {
                startObject = e.getObjectValue();
                break;
            }
            default: {
                throw new AssertionError(start.name());
            }
        }

        Object rv = startObject;
        for (Part part : parts) {
            rv = part.eval(rv);
        }

        return rv;
    }

    private static Part[] parseExpression(String[] tokens, String expression) {
        Part[] parts = new Part[tokens.length - 1];

        for (int i = 1; i < tokens.length; i++) {
            String token = tokens[i];

            boolean method = false;
            if (token.endsWith("()")) {
                method = true;
                token = token.substring(0, token.length() - 2);
            }
            verifyToken(token, expression);

            if (method) {
                parts[i - 1] = new MethodPart(token);
            } else {
                parts[i - 1] = new FieldPart(token);
            }
        }

        return parts;
    }

    private static void verifyToken(String token, String expression) {
        if (token.length() == 0) {
            throw new InvalidConfigurationException("Empty element in expression: " + expression);
        }

        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (i == 0) {
                if (!Character.isJavaIdentifierStart(c)) {
                    throw new InvalidConfigurationException("Invalid element (" + token + ") in expression: " + expression);
                }
            } else {
                if (!Character.isJavaIdentifierPart(c)) {
                    throw new InvalidConfigurationException("Invalid element (" + token + ") in expression: " + expression);
                }
            }
        }
    }

    /**
     * The various types of the start of the expression
     */
    private enum StartType {
        ELEMENT, VALUE, KEY;
    }

    /**
     * A part (method or field) of the expression
     */
    private interface Part extends Serializable {
        Object eval(Object target);
    }

    /**
     * A field expression part
     */
    private static class FieldPart implements Part {

        private final String fieldName;

        private transient volatile FieldRef cache;

        public FieldPart(String field) {
            this.fieldName = field;
        }

        public Object eval(Object target) {
            if (target == null) {
                throw new AttributeExtractorException("null reference encountered trying to read field " + fieldName);
            }

            Class c = target.getClass();
            FieldRef ref = cache;

            if (ref == null || ref.target != c) {
                while (true) {
                    try {
                        Field field = c.getDeclaredField(fieldName);
                        field.setAccessible(true);
                        ref = new FieldRef(target.getClass(), field);
                        cache = ref;
                        break;
                    } catch (NoSuchFieldException e) {
                        c = c.getSuperclass();
                        if (c == null) {
                            throw new AttributeExtractorException("No such field named \"" + fieldName + "\" present in instance of "
                                    + target.getClass());
                        }
                    } catch (Exception e) {
                        throw new AttributeExtractorException(e);
                    }
                }
            }

            try {
                return ref.field.get(target);
            } catch (Exception e) {
                throw new AttributeExtractorException(e);
            }
        }

    }

    /**
     * A reference to a resolved Field instance
     */
    private static class FieldRef {
        private final Class target;
        private final Field field;

        FieldRef(Class target, Field field) {
            this.target = target;
            this.field = field;
        }
    }

    /**
     * A reference to a resolved Method instance
     */
    private static class MethodRef {
        private final Method method;
        private final Class target;

        MethodRef(Class target, Method method) {
            this.target = target;
            this.method = method;
        }
    }

    /**
     * A method expression part
     */
    private static class MethodPart implements Part {

        private final String methodName;
        private transient volatile MethodRef cache;

        public MethodPart(String method) {
            this.methodName = method;
        }

        public Object eval(Object target) {
            if (target == null) {
                throw new AttributeExtractorException("null reference encountered trying to call " + methodName + "()");
            }

            Class c = target.getClass();

            MethodRef ref = cache;

            if (ref == null || ref.target != c) {
                while (true) {
                    try {
                        Method method = c.getDeclaredMethod(methodName);
                        method.setAccessible(true);
                        ref = new MethodRef(target.getClass(), method);
                        cache = ref;
                        break;
                    } catch (NoSuchMethodException e) {
                        c = c.getSuperclass();
                        if (c == null) {
                            throw new AttributeExtractorException("No such method named \"" + methodName + "\" present on instance of "
                                    + target.getClass());
                        }
                    } catch (Exception e) {
                        throw new AttributeExtractorException(e);
                    }
                }
            }

            try {
                return ref.method.invoke(target);
            } catch (InvocationTargetException e) {
                throw new AttributeExtractorException(e.getTargetException());
            } catch (Exception e) {
                throw new AttributeExtractorException(e);
            }
        }
    }
}
