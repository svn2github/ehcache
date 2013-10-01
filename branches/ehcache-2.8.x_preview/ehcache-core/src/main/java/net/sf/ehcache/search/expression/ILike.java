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

package net.sf.ehcache.search.expression;

import java.util.Map;
import java.util.regex.Pattern;

import net.sf.ehcache.Element;
import net.sf.ehcache.search.SearchException;
import net.sf.ehcache.search.attribute.AttributeExtractor;

/**
 * A regular expression criteria that matches attribute string values. For non <code>java.lang.String attributes</code>,
 * the <code>toString()</code> form is used in the comparison. <br>
 * <br>
 * Expressions are <b>always case insensitive</b><br>
 * <br>
 * The following special characters are supported:<br>
 * <ul>
 * <li>'?' - match any one single character
 * <li>'*' - match any multiple character(s) (including zero)
 * </ul>
 * The supported wildcard characters can be escaped with a backslash '\', and a literal backslash can be included with '\\'<br>
 * <br>
 * WARN: Expressions starting with a leading wildcard character are potentially very expensive (ie. full scan) for indexed caches
 * <p/>
 *
 * @author teck
 */
public class ILike extends BaseCriteria {

    private final String attributeName;
    private final String regex;
    private final Pattern pattern;

    /**
     * Construct a "like" criteria for the given expression
     *
     * @param attributeName attribute name
     * @param regex expression
     */
    public ILike(String attributeName, String regex) {
        if ((attributeName == null) || (regex == null)) {
            throw new SearchException("Both the attribute name and regex must be non null.");
        }

        this.attributeName = attributeName;
        this.regex = regex;
        this.pattern = convertRegex(regex.trim());
    }

    /**
     * Return attribute name.
     *
     * @return String attribute name
     */
    public String getAttributeName() {
        return attributeName;
    }

    /**
     * Return regex string.
     *
     * @return String regex.
     */
    public String getRegex() {
        return regex;
    }

    private static Pattern convertRegex(final String expr) {
        if (expr.length() == 0) {
            throw new SearchException("Zero length regex");
        }

        StringBuilder javaRegex = new StringBuilder("^");

        boolean escape = false;
        for (int i = 0; i < expr.length(); i++) {
            char ch = expr.charAt(i);

            if (escape) {
                switch (ch) {
                    case '\\':
                    case '?':
                    case '*': {
                        javaRegex.append(Pattern.quote(lowerCase(ch)));
                        break;
                    }

                    default: {
                        throw new SearchException("Illegal escape character (" + ch + ") in regex: " + expr);
                    }
                }

                escape = false;
            } else {
                switch (ch) {
                    case '\\': {
                        escape = true;
                        break;
                    }
                    case '?': {
                        javaRegex.append(".");
                        break;
                    }
                    case '*': {
                        javaRegex.append(".*");
                        break;
                    }
                    default: {
                        javaRegex.append(Pattern.quote(lowerCase(ch)));
                    }
                }
            }
        }

        javaRegex.append("$");

        return Pattern.compile(javaRegex.toString(), Pattern.DOTALL);
    }

    private static String lowerCase(char ch) {
        // heeding the advice in Character.toLowerCase() and using String.toLowerCase() instead here
        return Character.toString(ch).toLowerCase();
    }

    /**
     * {@inheritDoc}
     */
    public boolean execute(Element e, Map<String, AttributeExtractor> attributeExtractors) {
        Object value = getExtractor(attributeName, attributeExtractors).attributeFor(e, attributeName);
        if (value == null) {
            return false;
        }

        String asString = value.toString().toLowerCase();

        return pattern.matcher(asString).matches();
    }

}
