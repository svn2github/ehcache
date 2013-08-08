/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.NoSuchElementException;

import net.sf.ehcache.search.parser.Token;

/**
 * This class is support stuff for delegate parsing.
 */
public class ParserSupport {

    /**
     * The Class StringPlusToken.
     */
    public static class StringPlusToken {

        /**
         * The token.
         */
        private Token token;

        /**
         * The string.
         */
        private String string;

        /**
         * Instantiates a new string plus token.
         *
         * @param t the t
         * @param s the s
         */
        public StringPlusToken(Token t, String s) {
            this.token = t;
            this.string = s;
        }

        /**
         * Gets the token.
         *
         * @return the token
         */
        public Token getToken() {
            return token;
        }

        /**
         * Gets the string.
         *
         * @return the string
         */
        public String getString() {
            return string;
        }
    }

    /**
     * Process quoted string. This handles special chars, like unicode, newlines, etc. More could be added.
     *
     * @param tok the tok
     * @param s   the string to parse. this is the string without enclosing single quotes.
     * @return the string
     * @throws CustomParseException the custom parse exception
     */
    public static String processQuotedString(Token tok, String s) throws CustomParseException {
        StringBuilder sb = new StringBuilder();
        try {
            for (int i = 1; i < s.length() - 1; i++) {
                char c = s.charAt(i);
                if (c == 92) { // backslash
                    c = s.charAt(++i);
                    switch (c) {
                        case 'r':
                            sb.append((char)13);
                            break;
                        case 'n':
                            sb.append((char)10);
                            break;
                        case 'u':
                            String tmp = "";
                            for (int j = 0; j < 4; j++) {
                                tmp = tmp + s.charAt(++i);
                            }
                            sb.append((char)Integer.parseInt(tmp));
                            break;
                        case 't':
                            sb.append((char)9);
                            break;
                        default:
                            sb.append(c);
                    }
                } else {
                    sb.append(c);
                }
            }
            return sb.toString();
        } catch (Throwable t) {
            throw CustomParseException.factory(tok, CustomParseException.Message.SINGLE_QUOTE);
        }
    }

    /**
     * Make enum from string. Use for enum casts.
     *
     * @param enumName  the enum name
     * @param valueName the value name
     * @return the object
     */
    public static Object makeEnumFromString(String enumName, String valueName) {
        try {
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Class<? extends Enum> enumClz = (Class<? extends Enum>)Class.forName(enumName);
            @SuppressWarnings("unchecked")
            Object obj = Enum.valueOf(enumClz, valueName);
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Make object from string. The class is instantiated using a constructor with a single string argument.
     * Used for object casts.
     *
     * @param clzName     the class name
     * @param stringValue the string value
     * @return the object
     */
    public static Object makeObjectFromString(String clzName, String stringValue) {
        try {
            Class<?> clz = Class.forName(clzName);
            Constructor<?> cons = clz.getConstructor(new Class<?>[] { String.class });
            Object obj = cons.newInstance(new Object[] { stringValue });
            return obj;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * More of these could easily be put in here..
     * If you want real ISO standard formatting, you need either Java 7
     * or JodaTime. See:
     * http://stackoverflow.com/questions/2201925/converting-iso8601-compliant-string-to-java-util-date
     */
    private static String[] formats = {
        "yyyy-MM-dd HH:mm:ss.SSS z",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss z",
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd z",
        "yyyy-MM-dd",
        "yyyy-MM-dd'T'HH:mm:ss.SSS z",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd z",
        "yyyy-MM-dd",
        "MM/dd/yyyy HH:mm:ss.SSS z",
        "MM/dd/yyyy HH:mm:ss.SSS",
        "MM/dd/yyyy HH:mm:ss z",
        "MM/dd/yyyy HH:mm:ss",
        "MM/dd/yyyy z",
        "MM/dd/yyyy",
    };

    /**
     * Variant date parse.
     *
     * @param s the s
     * @return the date
     * @throws ParseException the parse exception
     */
    public static Date variantDateParse(String s) throws net.sf.ehcache.search.parser.ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.setLenient(false);
        for (String attempt : formats) {
            try {
                sdf.applyPattern(attempt);
                return sdf.parse(s);
            } catch (ParseException e) {
            }
        }
        throw new net.sf.ehcache.search.parser.ParseException("Date parsing error. Acceptable formats include: " + Arrays.asList(formats));
    }
}
