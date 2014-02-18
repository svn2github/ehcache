/**
 * Copyright Terracotta, Inc. Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and limitations under the
 * License.
 */
package net.sf.ehcache.search.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

/**
 * This class is support stuff for delegate parsing.
 */
public class ParserSupport {

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
                if (c == '\\') { 
                    c = s.charAt(++i);
                    switch (c) {
                        case 'r':
                            sb.append('\r');
                            break;
                        case 'n':
                            sb.append('\n');
                            break;
                        case 'u':
                            String tmp = "";
                            for (int j = 0; j < 4; j++) {
                                tmp = tmp + s.charAt(++i);
                            }
                            sb.append((char)Integer.parseInt(tmp));
                            break;
                        case 't':
                            sb.append('\t');
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
    public static <T extends Enum<T>> Enum<T> makeEnumFromString(ClassLoader loader, String enumName, String valueName) {
        // Attempt to load enum class by name, then validate that it's really enum
        Class<?> realType;
        try {
          realType = loader.loadClass(enumName);
        } catch (ClassNotFoundException e) {
          throw new IllegalArgumentException(String.format("Unable to load class specified as name of enum %s: %s", 
                  enumName, e.getMessage()));
        }
        @SuppressWarnings("unchecked")
        T obj = Enum.valueOf((Class<T>)realType, valueName);
        return obj;
    }

    /**
     * More of these could easily be put in here..
     * If you want real ISO standard formatting, you need either Java 7
     * or JodaTime. See:
     * http://stackoverflow.com/questions/2201925/converting-iso8601-compliant-string-to-java-util-date
     */
    private static String[] formats = {
        "yyyy-MM-dd'T'HH:mm:ss.SSS z",
        "yyyy-MM-dd'T'HH:mm:ss.SSS",
        "yyyy-MM-dd'T'HH:mm:ss z",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd z",
        "yyyy-MM-dd",
        "yyyy-MM-dd HH:mm:ss.SSS z",
        "yyyy-MM-dd HH:mm:ss.SSS",
        "yyyy-MM-dd HH:mm:ss z",
        "yyyy-MM-dd HH:mm:ss",
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
