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

package net.sf.ehcache.pool.sizeof;

import net.sf.ehcache.util.FindBugsSuppressWarnings;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.print.attribute.standard.MediaSize;
import javax.xml.datatype.DatatypeConstants;

/**
 * Enum with all the flyweight types that we check for sizeOf measurements
 *
 * @author Alex Snaps
 */
@FindBugsSuppressWarnings("RC_REF_COMPARISON")
enum FlyweightType {

    /**
     * java.lang.Enum
     */
    ENUM(Enum.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * javax.print.attribute.EnumSyntax
     */
    ENUM_SYNTAX(javax.print.attribute.EnumSyntax.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.lang.Class
     */
    CLASS(Class.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    // XXX There is no nullipotent way of determining the interned status of a string
    // There are numerous String constants within the JDK (see list at http://docs.oracle.com/javase/7/docs/api/constant-values.html),
    // but enumerating all of them would lead to lots of == tests.
    /**
     * java.lang.String
     */
    //STRING(String.class) {
    //    @Override
    //    boolean isShared(final Object obj) { return obj == ((String)obj).intern(); }
    //},
    /**
     * java.lang.Boolean
     */
    BOOLEAN(Boolean.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Boolean.TRUE || obj == Boolean.FALSE; }
    },
    /**
     * java.lang.Byte
     */
    BYTE(Byte.class) {
        @Override
        boolean isShared(final Object obj) { return obj == Byte.valueOf((Byte)obj); }
    },
    /**
     * java.lang.Short
     */
    SHORT(Short.class) {
        @Override
        boolean isShared(final Object obj) {
            short value = ((Short)obj).shortValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Short.valueOf(value);
        }
    },
    /**
     * java.lang.Integer
     */
    INTEGER(Integer.class) {
        @Override
        boolean isShared(final Object obj) {
            int value = ((Integer)obj).intValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Integer.valueOf(value);
        }
    },
    /**
     * java.lang.Long
     */
    LONG(Long.class) {
        @Override
        boolean isShared(final Object obj) {
            long value = ((Long)obj).longValue();
            return value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE && obj == Long.valueOf(value);
        }
    },
    /**
     * java.math.BigInteger
     */
    BIGINTEGER(BigInteger.class) {
        @Override
        boolean isShared(final Object obj) {
            return obj == BigInteger.ZERO || obj == BigInteger.ONE || obj == BigInteger.TEN;
        }
    },
    /**
     * java.math.BigDecimal
     */
    BIGDECIMAL(BigDecimal.class) {
        @Override
        boolean isShared(final Object obj) {
            return obj == BigDecimal.ZERO || obj == BigDecimal.ONE || obj == BigDecimal.TEN;
        }
    },
    /**
     * java.math.MathContext
     */
    MATHCONTEXT(MathContext.class) {
        @Override
        boolean isShared(final Object obj) {
            return obj == MathContext.UNLIMITED || obj == MathContext.DECIMAL32 || obj == MathContext.DECIMAL64 || obj == MathContext.DECIMAL128;
        }
    },
    /**
     * java.lang.Character
     */
    CHARACTER(Character.class) {
        @Override
        boolean isShared(final Object obj) { return ((Character)obj).charValue() <= Byte.MAX_VALUE && obj == Character.valueOf((Character)obj); }
    },
    /**
     * java.lang.Character.UnicodeBlock
     */
    CHARACTER_UNICODEBLOCK(Character.UnicodeBlock.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.nio.charset.CodingErrorAction
     */
    CODINGERRORACTION(java.nio.charset.CodingErrorAction.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  java.lang.Locale
     */
    LOCALE(Locale.class) {
        @Override
        boolean isShared(final Object obj) {
            return GLOBAL_LOCALES.contains(obj);
        }
    },
    /**
     * java.util.Logger
     */
    LOGGER(java.util.logging.Logger.class) {
        @Override
        @SuppressWarnings("deprecation")
        boolean isShared(final Object obj) { return obj == java.util.logging.Logger.global; }
    },
    /**
     * java.net.Proxy
     */
    PROXY(java.net.Proxy.class) {
        @Override
        boolean isShared(final Object obj) { return obj == java.net.Proxy.NO_PROXY; }
    },
    /**
     * javax.xml.datatype.DatatypeConstants.Field
     */
    DATATYPECONSTANTS_FIELD(DatatypeConstants.Field.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * javax.xml.namespace.QName
     */
    QNAME(javax.xml.namespace.QName.class) {
        @Override
        boolean isShared(final Object obj) {
            return obj == DatatypeConstants.DATETIME
                    || obj == DatatypeConstants.TIME
                    || obj == DatatypeConstants.DATE
                    || obj == DatatypeConstants.GYEARMONTH
                    || obj == DatatypeConstants.GMONTHDAY
                    || obj == DatatypeConstants.GYEAR
                    || obj == DatatypeConstants.GMONTH
                    || obj == DatatypeConstants.GDAY
                    || obj == DatatypeConstants.DURATION
                    || obj == DatatypeConstants.DURATION_DAYTIME
                    || obj == DatatypeConstants.DURATION_YEARMONTH;
                    // Java 6:
//                    || obj == javax.xml.soap.SOAPConstants.SOAP_DATAENCODINGUNKNOWN_FAULT
//                    || obj == javax.xml.soap.SOAPConstants.SOAP_MUSTUNDERSTAND_FAULT
//                    || obj == javax.xml.soap.SOAPConstants.SOAP_RECEIVER_FAULT
//                    || obj == javax.xml.soap.SOAPConstants.SOAP_SENDER_FAULT
//                    || obj == javax.xml.soap.SOAPConstants.SOAP_VERSIONMISMATCH_FAULT;
        }
    },
    /**
     * java.awt.BufferCapabilities.FlipContents
     */
    BUFFERCAPABILITIES_FLIPCONTENTS(java.awt.BufferCapabilities.FlipContents.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.JobAttributes.DefaultSelectionType
     */
    JOBATTRIBUTES_DEFAULTSELECTIONTYPE(java.awt.JobAttributes.DefaultSelectionType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.JobAttributes.DestinationType
     */
    JOBATTRIBUTES_DESTINATIONTYPE(java.awt.JobAttributes.DestinationType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.JobAttributes.DialogType
     */
    JOBATTRIBUTES_DIALOGTYPE(java.awt.JobAttributes.DialogType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.JobAttributes.MultipleDocumentHandlingType
     */
    JOBATTRIBUTES_MULTIPLEDOCUMENTHANDLINGTYPE(java.awt.JobAttributes.MultipleDocumentHandlingType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.JobAttributes.SidesType
     */
    JOBATTRIBUTES_SIDESTYPE(java.awt.JobAttributes.SidesType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.PageAttributes.ColorType
     */
    PAGEATTRIBUTES_COLORTYPE(java.awt.PageAttributes.ColorType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.PageAttributes.MediaType
     */
    PAGEATTRIBUTES_MEDIATYPE(java.awt.PageAttributes.MediaType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.PageAttributes.OrientationRequestedType
     */
    PAGEATTRIBUTES_ORIENTATIONREQUESTEDTYPE(java.awt.PageAttributes.OrientationRequestedType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.PageAttributes.OriginType
     */
    PAGEATTRIBUTES_ORIGINTYPE(java.awt.PageAttributes.OriginType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * java.awt.PageAttributes.PrintQualityType
     */
    PAGEATTRIBUTES_PRINTQUALITYTYPE(java.awt.PageAttributes.PrintQualityType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  javax.print.attribute.standard.MediaSize
     */
    MEDIASIZE(MediaSize.class) {
        @Override
        boolean isShared(final Object obj) {
            return GLOBAL_MEDIASIZES.contains(obj);
        }
    },
    /**
     *  javax.swing.event.DocumentEvent.EventType
     */
    DOCUMENTEVENT_EVENTTYPE(javax.swing.event.DocumentEvent.EventType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  javax.swing.event.HyperlinkEvent.EventType
     */
    HYPERLINKEVENT_EVENTTYPE(javax.swing.event.HyperlinkEvent.EventType.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  javax.swing.text.html.CSS.Attribute
     */
    CSS_ATTRIBUTE(javax.swing.text.html.CSS.Attribute.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  javax.swing.text.html.HTML.Attribute
     */
    HTML_ATTRIBUTE(javax.swing.text.html.HTML.Attribute.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     *  javax.swing.text.Position.Bias
     */
    POSITION_BIAS(javax.swing.text.Position.Bias.class) {
        @Override
        boolean isShared(final Object obj) { return true; }
    },
    /**
     * misc comparisons that can not rely on the object's class.
     */
    MISC(Void.class) {
        @Override
        boolean isShared(final Object obj) {
            boolean emptyCollection = obj == Collections.EMPTY_SET || obj == Collections.EMPTY_LIST || obj == Collections.EMPTY_MAP;
            boolean systemStream = obj == System.in || obj == System.out || obj == System.err;
            return emptyCollection || systemStream || obj == String.CASE_INSENSITIVE_ORDER;
        }
    };

    private static final Map<Class<?>, FlyweightType> TYPE_MAPPINGS = new HashMap<Class<?>, FlyweightType>();
    static {
        for (FlyweightType type : FlyweightType.values()) {
          TYPE_MAPPINGS.put(type.clazz, type);
        }
    }

    private static <T> Set<T> getAllFields(Class<?> classToSearch, Class<T> fieldType) {
        Map<T, Void> result = new IdentityHashMap<T, Void>();
        for (Field f : classToSearch.getFields()) {
            int modifiers = f.getModifiers();
            if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers) && fieldType.equals(f.getType())) {
                try {
                    result.put((T) f.get(null), null);
                } catch (IllegalArgumentException e) {
                    continue;
                } catch (IllegalAccessException e) {
                    continue;
                }
            }
        }
        return result.keySet();
    }

    private static final Set<Locale> GLOBAL_LOCALES;
    private static final Set<javax.print.attribute.standard.MediaSize> GLOBAL_MEDIASIZES;
    static {
        GLOBAL_LOCALES = getAllFields(Locale.class, Locale.class);
        Set<MediaSize> allMediaSizes = new HashSet<MediaSize>();
        allMediaSizes.addAll(getAllFields(MediaSize.Engineering.class, MediaSize.class));
        allMediaSizes.addAll(getAllFields(MediaSize.ISO.class, MediaSize.class));
        allMediaSizes.addAll(getAllFields(MediaSize.JIS.class, MediaSize.class));
        allMediaSizes.addAll(getAllFields(MediaSize.NA.class, MediaSize.class));
        allMediaSizes.addAll(getAllFields(MediaSize.Other.class, MediaSize.class));
        GLOBAL_MEDIASIZES = allMediaSizes;
    }

    private final Class<?> clazz;

    private FlyweightType(final Class<?> clazz) {
        this.clazz = clazz;
    }

    /**
     * Whether this is a shared object
     * @param obj the object to check for
     * @return true, if shared
     */
    abstract boolean isShared(Object obj);

    /**
     * Will return the Flyweight enum instance for the flyweight Class, or null if type isn't flyweight
     * @param aClazz the class we need the FlyweightType instance for
     * @return the FlyweightType, or null
     */
    static FlyweightType getFlyweightType(final Class<?> aClazz) {
        if (aClazz.isEnum() || (aClazz.getSuperclass() != null && aClazz.getSuperclass().isEnum())) {
            return ENUM;
        } else if (javax.print.attribute.EnumSyntax.class.isAssignableFrom(aClazz)) {
            return ENUM_SYNTAX;
        } else {
            FlyweightType flyweightType = TYPE_MAPPINGS.get(aClazz);
            return flyweightType != null ? flyweightType : MISC;
        }
    }
}
